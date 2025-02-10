package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.*;

import java.util.Objects;
import java.util.function.Function;

import static de.flogehring.jetpack.parse.EvaluateOperators.*;
import static de.flogehring.jetpack.parse.EvaluateTerminal.applyTerminal;

public class Evaluate {

    public static Either<ConsumedExpression, String> applyRule(
            Expression expression,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        return switch (expression) {
            case Operator op -> applyOperator(
                    op,
                    input,
                    currentPosition,
                    createEvaluatorWithApplyRule(grammar, memoTable)
            );
            case Symbol sym -> applySymbol(
                    sym,
                    input,
                    currentPosition,
                    grammar,
                    memoTable
            );
        };
    }

    private static ExpressionEvaluator createEvaluatorWithApplyRule(Function<Symbol.NonTerminal, Expression> grammar, MemoTable memoTable) {
        return ((expression, input, currentPosition) -> applyRule(
                expression,
                input,
                currentPosition,
                grammar,
                memoTable
        )
        );
    }

    private static Either<ConsumedExpression, String> applySymbol(Symbol symbol, Input input, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar, MemoTable memoTable) {
        return switch (symbol) {
            case Symbol.Terminal(var t) -> applyTerminal(t, input, currentPosition);
            case Symbol.NonTerminal nonTerminal ->
                    applyNonterminal(input, currentPosition, grammar, memoTable, nonTerminal);
        };
    }

    private static Either<ConsumedExpression, String> applyNonterminal(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal
    ) {
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        return switch (memoTable.get(key)) {
            case MemoTableLookup.NoHit() ->
                    tryParseNonterminal(input, currentPosition, grammar, memoTable, nonTerminal);
            case MemoTableLookup.LeftRecursion(boolean _) -> {
                memoTable.setLeftRecursion(key, true);
                yield Either.or("Left Recursion detected");
            }
            case MemoTableLookup.Success(var position) -> Either.ofThis(new ConsumedExpression(position));
            case MemoTableLookup.PreviousParsingFailure() -> Either.or("Previous failure");
        };
    }

    private static Either<ConsumedExpression, String> tryParseNonterminal(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal
    ) {
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        memoTable.setLeftRecursion(key, false);
        Either<ConsumedExpression, String> applied = applyNonterminalWithoutLookup(
                input, currentPosition, grammar, memoTable, nonTerminal
        );
        boolean detected = memoTable.getLeftRecursion(key);
        if (detected && applied instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            memoTable.removeLeftRecursion(key);
            memoTable.insertSuccess(key, consumedExpression.parsePosition());
            applied = growLeftRecursion(
                    input, currentPosition, grammar, memoTable, nonTerminal
            );
            memoTable.removeLeftRecursion(key);
        }
        return applied;
    }

    private static Either<ConsumedExpression, String> growLeftRecursion(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal
    ) {
        Either<ConsumedExpression, String> evaluated = null;
        System.out.println("Grow Left Recursion called");
        int lastPosition = currentPosition;
        ExpressionEvaluator evaluator = createEvaluatorWithoutGrowLeftRecursion(
                grammar,
                memoTable
        );
        while (true) {
            Either<ConsumedExpression, String> evaluatedNonTerminal = evaluator.resolveExpression(
                    nonTerminal, input, currentPosition
            );
            if (evaluatedNonTerminal instanceof Either.This<ConsumedExpression, String>(
                    var consumedExpression
            )) {
                if (consumedExpression.parsePosition() <= lastPosition) {
                    System.out.println("Grow left recursion exited" + consumedExpression.parsePosition());
                    return Objects.requireNonNullElse(
                            evaluated, evaluatedNonTerminal
                    );
                }
                evaluated = evaluatedNonTerminal;
                lastPosition = consumedExpression.parsePosition();
                memoTable.insertSuccess(new MemoTableKey(nonTerminal.name(), currentPosition), consumedExpression.parsePosition());
            } else {
                System.out.println("Grow left recursion exited via failure" + evaluated.getEither().parsePosition());
                return evaluated;
            }
        }
    }


    private static Either<ConsumedExpression, String> evaluateNonterminal(
            Symbol.NonTerminal nonTerminal,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        Expression expression = grammar.apply(nonTerminal);
        return applyRule(expression, input, currentPosition, grammar, memoTable);
    }

    private static ExpressionEvaluator createEvaluatorWithoutGrowLeftRecursion(
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        return new ExpressionEvaluator() {
            @Override
            public Either<ConsumedExpression, String> resolveExpression(Expression expression, Input input, int currentPositio) {
                return switch (expression) {
                    case Operator operator -> EvaluateOperators.applyOperator(
                            operator,
                            input,
                            currentPositio,
                            createEvaluatorWithoutGrowLeftRecursion(grammar, memoTable)
                    );
                    case Symbol symbol -> switch (symbol) {
                        case Symbol.NonTerminal nonTerminal -> applyNonterminalWithoutLeftRecursion(
                                input,
                                currentPositio,
                                nonTerminal,
                                memoTable,
                                grammar
                        );
                        case Symbol.Terminal(var t) -> EvaluateTerminal.applyTerminal(
                                t, input, currentPositio
                        );
                    };
                };
            }

            private Either<ConsumedExpression, String> applyNonterminalWithoutLeftRecursion(
                    Input input,
                    int currentPositio, Symbol.NonTerminal nonTerminal,
                    MemoTable memoTable,
                    Function<Symbol.NonTerminal, Expression> grammar
            ) {
                MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPositio);
                MemoTableLookup lookupIgnoreLeftRecursion = memoTable.getLookupIgnoreLeftRecursion(key);
                Either<ConsumedExpression, String> result = switch (lookupIgnoreLeftRecursion) {
                    case MemoTableLookup.LeftRecursion _ -> throw new RuntimeException("Contract broken");
                    case MemoTableLookup.NoHit() -> {
                        memoTable.insertFailure(key);
                        yield this.resolveExpression(grammar.apply(nonTerminal), input, currentPositio);
                    }
                    case MemoTableLookup.Success(var consumed) -> Either.ofThis(new ConsumedExpression(consumed));
                    case MemoTableLookup.PreviousParsingFailure() -> Either.or("Previous failure");
                };
                switch (result) {
                    case Either.This<ConsumedExpression, String>(var consumedExpression) -> memoTable.insertSuccess(
                            key, consumedExpression.parsePosition()
                    );
                    case Either.Or<ConsumedExpression, String> _ ->
                        memoTable.insertFailure(key);
                }
                return result;
            }
        };
    }

    private static Either<ConsumedExpression, String> applyNonterminalWithoutLookup(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal
    ) {
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        Either<ConsumedExpression, String> consumedExpressionStringEither = applyRule(
                grammar.apply(nonTerminal),
                input,
                currentPosition,
                grammar,
                memoTable
        );
        if (consumedExpressionStringEither instanceof Either.This<ConsumedExpression, String>(var success)) {
            memoTable.insertSuccess(key, success.parsePosition());
        } else {
            memoTable.insertFailure(key);
        }
        return consumedExpressionStringEither;
    }
}