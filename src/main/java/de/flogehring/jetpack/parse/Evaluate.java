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
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        final LookupTable lookupTable = parsingState.getLookup();

        var lookup = lookupTable.get(key);
        return switch (lookup) {
            case MemoTableLookup.NoHit() -> {
                Expression ruleDef = grammar.apply(nonTerminal);
                lookupTable.insertFailure(key);
                var ans = evaluateExpression(ruleDef, input, currentPosition, grammar, parsingState);
                switch (ans) {
                    case Either.This<ConsumedExpression, String>(var consumed) ->
                            lookupTable.insertSuccess(key, consumed.parsePosition());
                    case Either.Or<ConsumedExpression, String> _ -> lookupTable.insertFailure(key);
                }
                yield ans;
            }
            case MemoTableLookup.Success(var position) -> Either.ofThis(new ConsumedExpression(position));
            case MemoTableLookup.Fail() -> Either.or("Previous failure");
        };
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