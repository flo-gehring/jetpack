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
                    grammar,
                    memoTable
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

    private static Either<ConsumedExpression, String> evaluateWithoutLeftRecursionDetection(
            Expression expression,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        return null;
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
        if (detected && applied instanceof Either.This<ConsumedExpression, String>(var _)) {

            applied = growLeftRecursion(
                    input, currentPosition, grammar, memoTable, nonTerminal
            );
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
        while (true) {
            Either<ConsumedExpression, String> evaluatedNonTerminal = evaluateNonterminal(
                    nonTerminal, input, currentPosition, grammar, memoTable
            );
            if (evaluatedNonTerminal instanceof Either.This<ConsumedExpression, String>(
                    var consumedExpression
            )) {
                if (consumedExpression.parsePosition() <= currentPosition) {
                    return Objects.requireNonNullElse(
                            evaluated, evaluatedNonTerminal
                    );
                }
                evaluated = evaluatedNonTerminal;
                memoTable.insertSuccess(new MemoTableKey(nonTerminal.name(), currentPosition), consumedExpression.parsePosition());
            } else {
                return evaluatedNonTerminal;
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