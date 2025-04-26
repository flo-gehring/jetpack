package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.datatypes.MemoTable;
import de.flogehring.jetpack.datatypes.MemoTableLookup;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static de.flogehring.jetpack.parse.EvaluateOperators.applyOperator;
import static de.flogehring.jetpack.parse.EvaluateTerminal.applyTerminal;

/**
 * This class is capable of evaluating left recursive parsing expression grammars (PEG).
 * It follows the proposed method of
 * <a href="https://www.jstage.jst.go.jp/article/ipsjjip/29/0/29_174/_pdf"> Umeda and Maeda Paper "Packrat Parsers Can Support Multiple Left-recursive
 * Calls at the Same Position"</a>
 */
class Evaluate {

    private Evaluate() {
    }

    public static Either<ConsumedExpression, String> evaluate(
            Input input,
            String startingRule,
            Function<Symbol.NonTerminal, Expression> grammar
    ) {
        return evaluateExpressionWithApplyRule(
                Expression.nonTerminal(startingRule),
                input,
                0,
                grammar,
                ParsingState.of()
        );
    }

    private static Either<ConsumedExpression, String> evaluateExpressionWithApplyRule(
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
            case Symbol sym -> applySymbolNonRecursive(
                    sym,
                    input,
                    currentPosition,
                    grammar,
                    parsingState
            );
        };
    }

    private static Either<ConsumedExpression, String> applySymbolNonRecursive(
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
        final MemoTable<MemoTableKey, ParsingStateLookup> memoTable = parsingState.getLookup();
        final MemoTableLookup<ParsingStateLookup> lookup = memoTable.get(key);
        return switch (lookup) {
            case MemoTableLookup.NoHit<ParsingStateLookup>() -> {
                memoTable.insert(key, new ParsingStateLookup.Fail(false));
                Expression ruleDef = grammar.apply(nonTerminal);
                Either<ConsumedExpression, String> answer = evaluateExpressionWithApplyRule(
                        ruleDef,
                        input,
                        currentPosition,
                        grammar,
                        parsingState
                );
                updateState(key, answer, parsingState);
                ParsingStateLookup m = ((MemoTableLookup.Hit<ParsingStateLookup>) memoTable.get(key)).value();
                if (m.growLr()) {
                    answer = growLr(
                            nonTerminal,
                            input,
                            currentPosition,
                            grammar,
                            parsingState
                    );
                    updateState(key, answer, parsingState);
                }
                yield answer.map(
                        consumedExpression -> new ConsumedExpression(
                                consumedExpression.parsePosition(),
                                List.of(Node.of(nonTerminal, consumedExpression.parseTree()))
                        )
                );
            }
            case MemoTableLookup.Hit<ParsingStateLookup>(var entry) -> {
                Either<ConsumedExpression, String> answer;
                if (entry instanceof ParsingStateLookup.Fail(var _)) {
                    memoTable.insert(key, new ParsingStateLookup.MisMatch(
                            true
                    ));
                    answer = Either.or("Detected Left recursion");
                } else {
                    answer = switch (entry) {
                        case ParsingStateLookup.MisMatch _ -> Either.or("Previous parsing failure");
                        case ParsingStateLookup.Match(var parsePosition, var tree, var _) ->
                                Either.ofThis(new ConsumedExpression(parsePosition, tree));
                        case ParsingStateLookup.Fail ignored -> throw new RuntimeException();
                    };
                }
                yield answer.map(
                        consumedExpression -> new ConsumedExpression(
                                consumedExpression.parsePosition(),
                                List.of(Node.of(nonTerminal, consumedExpression.parseTree()))
                        ));
            }
        };
    }

    private static void updateState(
            MemoTableKey key,
            Either<ConsumedExpression, String> answer,
            ParsingState parsingState
    ) {
        MemoTable<MemoTableKey, ParsingStateLookup> lookup = parsingState.getLookup();
        ParsingStateLookup entry = ((MemoTableLookup.Hit<ParsingStateLookup>) lookup.get(key)).value();
        switch (answer) {
            case Either.This<ConsumedExpression, String>(var consumedExpression) -> lookup.insert(
                    key,
                    new ParsingStateLookup.Match(
                            consumedExpression.parsePosition(),
                            consumedExpression.parseTree(),
                            entry.growLr()
                    )
            );
            case Either.Or<ConsumedExpression, String>(var _) -> lookup.insert(
                    key, new ParsingStateLookup.MisMatch(entry.growLr())
            );
        }
    }

    private static Either<ConsumedExpression, String> growLr(
            Symbol.NonTerminal nonTerminal,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState
    ) {
        Expression exp = grammar.apply(nonTerminal);
        Either<ConsumedExpression, String> answer = Either.or(
                "Unable to evaluate recursive call at " + currentPosition
        );
        int oldPosition = currentPosition;
        while (true) {
            Either<ConsumedExpression, String> currentAnswer = evalGrow(
                    exp,
                    input,
                    currentPosition,
                    grammar,
                    parsingState,
                    new HashSet<>(List.of(nonTerminal))
            );
            if (currentAnswer instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
                if (consumedExpression.parsePosition() <= oldPosition) {
                    break;
                }
                oldPosition = consumedExpression.parsePosition();
            } else if (currentAnswer instanceof Either.Or<ConsumedExpression, String>) {
                break;
            }
            updateState(new MemoTableKey(nonTerminal.name(), currentPosition), currentAnswer, parsingState);
            answer = currentAnswer;
        }
        return answer;
    }

    private static Either<ConsumedExpression, String> evalGrow(
            Expression exp,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState,
            Set<Symbol.NonTerminal> limits
    ) {
        ExpressionEvaluator evalGrowEvaluator = getEvalGrowEvaluator(
                limits,
                parsingState,
                grammar,
                currentPosition
        );
        return evalGrowEvaluator.resolveExpression(
                exp,
                input,
                currentPosition
        );
    }

    private static ExpressionEvaluator getEvalGrowEvaluator(
            Set<Symbol.NonTerminal> limits,
            ParsingState parsingState,
            Function<Symbol.NonTerminal, Expression> grammar,
            int startingPositionForEvalGrowCall
    ) {
        return (expression, input1, currentPosition1) ->
                switch (expression) {
                    case Operator op -> EvaluateOperators.applyOperator(
                            op,
                            input1,
                            currentPosition1,
                            getEvalGrowEvaluator(limits, parsingState, grammar, startingPositionForEvalGrowCall)

                    );
                    case Symbol s -> switch (s) {
                        case Symbol.Terminal(var t) -> EvaluateTerminal.applyTerminal(
                                t,
                                input1,
                                currentPosition1
                        );
                        case Symbol.NonTerminal current -> evalGrowNonTerminal(
                                input1,
                                current,
                                currentPosition1,
                                limits,
                                parsingState,
                                grammar,
                                startingPositionForEvalGrowCall
                        );
                    };
                };
    }

    private static Either<ConsumedExpression, String> evalGrowNonTerminal(
            Input input,
            Symbol.NonTerminal current,
            int currentPosition,
            Set<Symbol.NonTerminal> limits,
            ParsingState parsingState,
            Function<Symbol.NonTerminal, Expression> grammar,
            int startingPositionForEvalGrowCall
    ) {
        if (currentPosition == startingPositionForEvalGrowCall && !limits.contains(current)) {
            return applyRuleGrowRecursive(
                    input,
                    current,
                    limits,
                    currentPosition,
                    grammar,
                    parsingState
            );
        } else {
            return applyRule(
                    input,
                    currentPosition,
                    grammar,
                    parsingState,
                    current
            );
        }
    }

    private static Either<ConsumedExpression, String> applyRuleGrowRecursive(
            Input input,
            Symbol.NonTerminal nonTerminal,
            Set<Symbol.NonTerminal> limits,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState
    ) {
        limits.add(nonTerminal);
        Expression ruleDef = grammar.apply(nonTerminal);
        Either<ConsumedExpression, String> answer = evalGrow(
                ruleDef,
                input,
                currentPosition,
                grammar,
                parsingState,
                limits
        );
        MemoTableKey key = new MemoTableKey(
                nonTerminal.name(),
                currentPosition
        );
        MemoTableLookup<ParsingStateLookup> previousLookup = parsingState.getLookup().get(key);
        int previousLookupParsePosition = getPreviousLookupParsePositionOrDefault(currentPosition, previousLookup);
        if (answer instanceof Either.Or<ConsumedExpression, String>) {
            answer = getAnswerFromPreviousLookupGrowLr(previousLookup);
        } else if (answer instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            if (consumedExpression.parsePosition() <= previousLookupParsePosition) {
                answer = getAnswerFromPreviousLookupGrowLr(previousLookup);
            } else {
                updateState(
                        key,
                        answer,
                        parsingState
                );
            }
        }
        return answer.map(consumedExpression -> new ConsumedExpression(
                consumedExpression.parsePosition(),
                List.of(Node.of(nonTerminal, consumedExpression.parseTree()))
        ));
    }

    private static int getPreviousLookupParsePositionOrDefault(
            int defaultPosition,
            MemoTableLookup<ParsingStateLookup> previousLookup
    ) {
        return switch (previousLookup) {
            case MemoTableLookup.NoHit<ParsingStateLookup> _ -> defaultPosition;
            case MemoTableLookup.Hit<ParsingStateLookup> hit ->
                    hit.value() instanceof ParsingStateLookup.Match(var parsePosition, var _, var _)
                            ? parsePosition
                            : defaultPosition;
        };
    }

    private static Either<ConsumedExpression, String> getAnswerFromPreviousLookupGrowLr(MemoTableLookup<ParsingStateLookup> previousLookup) {
        return switch (previousLookup) {
            case MemoTableLookup.NoHit<ParsingStateLookup> _ -> Either.or("No Previous hit");
            case MemoTableLookup.Hit<ParsingStateLookup>(var entry) -> switch (entry) {
                case ParsingStateLookup.Match(int parsePosition, var parseTree, var _) -> Either.ofThis(
                        new ConsumedExpression(parsePosition, parseTree)
                );
                case ParsingStateLookup.Fail _, ParsingStateLookup.MisMatch _ -> Either.or(
                        "Previous parsing failure"
                );
            };
        };
    }

    private static ExpressionEvaluator createEvaluatorWithApplyRule(
            Function<Symbol.NonTerminal, Expression> grammar,
            ParsingState parsingState
    ) {
        return (expression, input, currentPosition) -> evaluateExpressionWithApplyRule(
                expression,
                input,
                currentPosition,
                grammar,
                parsingState
        );
    }
}