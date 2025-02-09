package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.*;

import java.util.function.Function;

import static de.flogehring.jetpack.parse.EvaluateOperators.*;

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

    private static Either<ConsumedExpression, String> applySymbol(Symbol sym, Input input, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar, MemoTable memoTable) {
        return switch (sym) {
            case Symbol.Terminal(var t) -> EvaluateTerminal.applyTerminal(
                    t,
                    input,
                    currentPosition
            );
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
                    applyNonterminalWithoutLookup(input, currentPosition, grammar, memoTable, nonTerminal);
            case MemoTableLookup.Success(var position) -> Either.ofThis(new ConsumedExpression(position));
            case MemoTableLookup.PreviousParsingFailure() -> Either.or("Previous failure");

        };
    }

    private static Either<ConsumedExpression, String> applyNonterminalWithoutLookup(Input input, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar, MemoTable memoTable, Symbol.NonTerminal nonTerminal) {
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        memoTable.insertFailure(key);
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