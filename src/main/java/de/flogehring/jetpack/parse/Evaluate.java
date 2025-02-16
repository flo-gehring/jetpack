package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.*;

import java.util.Stack;
import java.util.function.Function;

import static de.flogehring.jetpack.parse.EvaluateOperators.applyOperator;
import static de.flogehring.jetpack.parse.EvaluateTerminal.applyTerminal;

public class Evaluate {

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
        final LookupTable lookupTable = parsingState.getLookup();
        final MemoTableLookup lookup = lookupTable.get(key);
        final Stack<Symbol.NonTerminal> callStack = parsingState.getCallStack();
        boolean memoTableHit = !(lookup instanceof MemoTableLookup.NoHit);
        if (memoTableHit && callStack.search(nonTerminal) != -1) {
            return switch (lookup) {
                case MemoTableLookup.Success(var consumed) -> Either.ofThis(new ConsumedExpression(consumed));
                case MemoTableLookup.Fail() -> Either.or("Previous Parsing Failure");
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
        LookupTable memoTable = parsingState.getLookup();
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        MemoTableLookup lookup = memoTable.get(key);
        if (lookup instanceof MemoTableLookup.NoHit) {
            memoTable.insertFailure(key);
        }
        Expression ruleDef = grammar.apply(nonTerminal);
        var ans = evaluateExpression(ruleDef, input, currentPosition, grammar, parsingState);
        int position = currentPosition;
        if (ans instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            if (parsingState.getMaxPos() < consumedExpression.parsePosition()) {
                parsingState.setMaxPos(consumedExpression.parsePosition());
            }
            memoTable.insertSuccess(key, consumedExpression.parsePosition());
            position = consumedExpression.parsePosition();
        } else {
            memoTable.insertFailure(key);
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
        }
        return ans;
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
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        if (ans instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            parsingState.getLookup().insertSuccess(key, consumedExpression.parsePosition());
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