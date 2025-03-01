package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.datatypes.Tuple;
import de.flogehring.jetpack.grammar.*;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Function;

import static de.flogehring.jetpack.parse.EvaluateOperators.applyOperator;
import static de.flogehring.jetpack.parse.EvaluateTerminal.applyTerminal;

public class Evaluate {

    private Evaluate() {
    }

    public static Either<ConsumedExpression, String> evaluate(
            Input input,
            String startingRule,
            Function<Symbol.NonTerminal, Expression> grammar
    ) {
        return evaluateExpression(
                Expression.nonTerminal(startingRule),
                input,
                0,
                grammar,
                ParsingState.of()
        );
    }

    private static Either<ConsumedExpression, String> evaluateExpression(
            Expression expression,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState
    ) {
        return switch (expression) {
            case Operator op -> applyOperator(
                    op,
                    input,
                    currentPosition,
                    createEvaluatorWithApplyRule(grammar, parsingState)
            );
            case Symbol sym -> applySymbol(
                    sym,
                    input,
                    currentPosition,
                    grammar,
                    parsingState
            );
        };
    }

    private static Either<ConsumedExpression, String> applySymbol(
            Symbol symbol,
            Input input,
            int currentPosition, Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState
    ) {
        return switch (symbol) {
            case Symbol.Terminal(var t) -> applyTerminal(t, input, currentPosition);
            case Symbol.NonTerminal nonTerminal -> applyRule(
                    input,
                    currentPosition,
                    grammar,
                    parsingState,
                    nonTerminal
            );
        };
    }

    private static Either<ConsumedExpression, String> applyRule(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState,
            Symbol.NonTerminal nonTerminal
    ) {
        final MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        final MemoTable memoTable = parsingState.getLookup();
        final MemoTableLookup lookup = memoTable.get(key);
        final Stack<Symbol.NonTerminal> callStack = parsingState.getCallStack();
        boolean memoTableHit = !(lookup instanceof MemoTableLookup.NoHit);
        if (memoTableHit && callStack.search(nonTerminal) != -1) {
            return switch (lookup) {
                case MemoTableLookup.Success(var offset, var parseTree, var _) -> Either.ofThis(
                        new ConsumedExpression(offset, parseTree)
                );
                case MemoTableLookup.Fail(var _) -> Either.or("Previous Parsing Failure");
                case MemoTableLookup.NoHit() -> throw new RuntimeException("Unreachable State");
            };
        }
        return updateMemo(
                nonTerminal, input, currentPosition, grammar, parsingState
        );
    }

    private static Either<ConsumedExpression, String> updateMemo(
            Symbol.NonTerminal nonTerminal,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState
    ) {
        parsingState.getCallStack().push(nonTerminal);
        MemoTable memoTable = parsingState.getLookup();
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        MemoTableLookup lookup = memoTable.get(key);
        if (lookup instanceof MemoTableLookup.NoHit) {
            memoTable.insertFailure(key, false);
        }
        Expression ruleDef = grammar.apply(nonTerminal);
        var ans = evaluateExpression(ruleDef, input, currentPosition, grammar, parsingState);
        int position = currentPosition;
        if (ans instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            if (parsingState.getMaxPos() < consumedExpression.parsePosition()) {
                parsingState.setMaxPos(consumedExpression.parsePosition());
            }
            memoTable.insertSuccess(key,
                    consumedExpression.parsePosition(),
                    consumedExpression.parseTree(),
                    false
            );
            position = consumedExpression.parsePosition();
        } else {
            memoTable.insertFailure(key, false);
            ans = Either.or(
                    createError(parsingState, input)
            );
        }
        parsingState.getCallStack().pop();
        if (!parsingState.isGrowState()
                && parsingState.getCallStack().empty()
                && position < input.length()
        ) {
            while (true) {
                ans = growLr(
                        nonTerminal, input, currentPosition, grammar, parsingState
                );
                if (ans instanceof Either.Or<ConsumedExpression, String>) {
                    break;
                }
                if (ans instanceof Either.This<ConsumedExpression, String>(
                        var consumedExpression
                )) {
                    if (consumedExpression.parsePosition() >= input.length()) break;
                    if (position >= parsingState.getMaxPos()) break;
                    position = consumedExpression.parsePosition();
                }
            }
            return ans;
        } else {
            return ans.map(consumed -> new ConsumedExpression(
                    consumed.parsePosition(),
                    List.of(Node.of(nonTerminal, consumed.parseTree()))
            ));
        }
    }

    private static String createError(ParsingState parsingState, Input input) {
        MemoTable lookup = parsingState.getLookup();
        Optional<MemoTableKey> highestSuccess = lookup.getHighestSuccess();
        Optional<Integer> ruleOffset = highestSuccess.map(lookup::get).map(MemoTableLookup.Success.class::cast).map(MemoTableLookup.Success::offset);
        Optional<Tuple<String, String>> parsedAndUnparsedInput = ruleOffset.map(
                input::splitInput
        );
        return MessageFormat.format(
                """
                        Parsing Error:
                            - Highest Parse {0}
                            - Last Succesfull Rule: {1} at {2} -> {3}
                            - Split Input into:
                                        Parsed: {4}
                                        Unparsed:  {5}
                        """,
                parsingState.getMaxPos(),
                highestSuccess.map(MemoTableKey::name).orElse("No Match"),
                highestSuccess.map(MemoTableKey::position).map(String::valueOf).orElse("n/a"),
                ruleOffset.map(String::valueOf).orElse("n/a"),
                parsedAndUnparsedInput.map(Tuple::first),
                parsedAndUnparsedInput.map(Tuple::second)

        );
    }

    private static Either<ConsumedExpression, String> growLr(
            Symbol.NonTerminal nonTerminal,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState
    ) {
        parsingState.getCallStack().push(nonTerminal);
        parsingState.setGrowState(true);
        Expression exp = grammar.apply(nonTerminal);
        Either<ConsumedExpression, String> ans = evaluateExpression(exp, input, currentPosition, grammar, parsingState);
        ans = ans.map(consumedExpression -> new ConsumedExpression(
                consumedExpression.parsePosition(), List.of(
                Node.of(nonTerminal, consumedExpression.parseTree())
        )
        ));
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        if (ans instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            parsingState.getLookup().insertSuccess(
                    key,
                    consumedExpression.parsePosition(),
                    consumedExpression.parseTree(),
                    false
            );
            if (consumedExpression.parsePosition() <= currentPosition) {
                ans = Either.or("No Progress made");
            }
        }
        parsingState.setGrowState(false);
        parsingState.getCallStack().pop();
        return ans;
    }

    private static ExpressionEvaluator createEvaluatorWithApplyRule(
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState
    ) {
        return (expression, input, currentPosition) -> evaluateExpression(
                expression,
                input,
                currentPosition,
                grammar,
                parsingState
        );
    }
}