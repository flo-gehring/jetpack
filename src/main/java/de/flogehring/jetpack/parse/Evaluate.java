package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.*;

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
                MemoTable.of()
        );
    }

    private static Either<ConsumedExpression, String> evaluateExpression(
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

    private static Either<ConsumedExpression, String> applySymbol(
            Symbol symbol,
            Input input,
            int currentPosition, Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        return switch (symbol) {
            case Symbol.Terminal(var t) -> applyTerminal(t, input, currentPosition);
            case Symbol.NonTerminal nonTerminal -> applyRule(
                    input,
                    currentPosition,
                    grammar,
                    memoTable,
                    nonTerminal
            );
        };
    }

    private static Either<ConsumedExpression, String> applyRule(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal
    ) {
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        var lookup = memoTable.lookup(key);
        return switch (lookup) {
            case MemoTableLookup.NoHit() -> {
                Expression ruleDef = grammar.apply(nonTerminal);
                var ans = evaluateExpression(ruleDef, input, currentPosition, grammar, memoTable);
                switch (ans) {
                    case Either.This<ConsumedExpression, String>(var consumed) -> {
                        memoTable.insertSuccess(key, consumed.parsePosition());
                    }
                    case Either.Or<ConsumedExpression, String> _ -> memoTable.insertFailure(key);
                }
                yield ans;
            }
            case MemoTableLookup.Success(var position) -> Either.ofThis(new ConsumedExpression(position));
            case MemoTableLookup.Fail() -> Either.or("Previous failure");
        };
    }

    private static ExpressionEvaluator createEvaluatorWithApplyRule(
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        return (expression, input, currentPosition) -> evaluateExpression(
                expression,
                input,
                currentPosition,
                grammar,
                memoTable
        );
    }

}