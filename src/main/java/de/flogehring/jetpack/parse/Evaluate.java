package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.*;

import java.util.Objects;
import java.util.function.Function;

import static de.flogehring.jetpack.parse.EvaluateOperators.*;
import static de.flogehring.jetpack.parse.EvaluateTerminal.applyTerminal;

public class Evaluate {

    public static Either<ConsumedExpression, String> evaluate(
            Input input,
            String startingRule,
            Function<Symbol.NonTerminal, Expression> grammar
    ) {
        return applyRule(
                Expression.nonTerminal(startingRule),
                input,
                0,
                grammar,
                MemoTable.of(),
                Heads.of(),
                Stack.empty()
        );
    }

    private static Either<ConsumedExpression, String> applyRule(
            Expression expression,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Heads heads,
            Stack<Symbol.NonTerminal> ruleInvocationStack
    ) {
        return switch (expression) {
            case Operator op -> applyOperator(
                    op,
                    input,
                    currentPosition,
                    createEvaluatorWithApplyRule(grammar, memoTable, heads, ruleInvocationStack)
            );
            case Symbol sym -> applySymbol(
                    sym,
                    input,
                    currentPosition,
                    grammar,
                    memoTable,
                    heads,
                    ruleInvocationStack
            );
        };
    }

    private static ExpressionEvaluator createEvaluatorWithApplyRule(
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Heads heads, Stack<Symbol.NonTerminal> ruleInvocationStack) {
        return (expression, input, currentPosition) -> applyRule(
                expression,
                input,
                currentPosition,
                grammar,
                memoTable,
                heads,
                ruleInvocationStack
        );
    }

    private static Either<ConsumedExpression, String> applySymbol(
            Symbol symbol,
            Input input,
            int currentPosition, Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Heads heads,
            Stack<Symbol.NonTerminal> ruleInvocationStack
    ) {
        return switch (symbol) {
            case Symbol.Terminal(var t) -> applyTerminal(t, input, currentPosition);
            case Symbol.NonTerminal nonTerminal -> applyNonterminal(
                    input,
                    currentPosition,
                    grammar,
                    memoTable,
                    nonTerminal,
                    heads,
                    ruleInvocationStack
            );
        };
    }

    private static Either<ConsumedExpression, String> applyNonterminal(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal,
            Heads heads,
            Stack<Symbol.NonTerminal> ruleInvocationStack
    ) {
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        MemoTableLookup recall = recall(input, heads, memoTable, nonTerminal, currentPosition, grammar, ruleInvocationStack);
        return switch (recall) {
            case MemoTableLookup.NoHit() ->
                    tryParseNonterminal(input, currentPosition, grammar, memoTable, nonTerminal,heads, ruleInvocationStack);
            case MemoTableLookup.LeftRecursion(boolean _) -> {
                if (memoTable.getInGrowLeftRecursion(key) && memoTable.getLookupIgnoreLeftRecursion(key) instanceof MemoTableLookup.Success(
                        var consumed
                )) {
                    yield Either.ofThis(new ConsumedExpression(consumed));
                } else {
                    //memoTable.setLeftRecursionDetected(key, true);
                    yield Either.or("Left Recursion detected");
                }
            }
            case MemoTableLookup.Success(var position) -> Either.ofThis(new ConsumedExpression(position));
            case MemoTableLookup.PreviousParsingFailure() -> Either.or("Previous failure");
        };
    }

    private static MemoTableLookup recall(
            Input input,
            Heads heads,
            MemoTable memoTable,
            Symbol.NonTerminal rule,
            int position,
            Function<Symbol.NonTerminal, Expression> grammar,
            Stack<Symbol.NonTerminal> ruleInvocationStack

    ) {
        String name = rule.name();
        MemoTableKey memoTableKey = new MemoTableKey(name, position);
        var lookup = memoTable.get(memoTableKey);
        return heads.get(position).map(
                head -> recallWithHead(
                        input,
                        memoTable,
                        rule,
                        position,
                        grammar,
                        head,
                        lookup,
                        name,
                        heads,
                        ruleInvocationStack
                )
        ).orElse(lookup);
    }

    private static MemoTableLookup recallWithHead(
            Input input,
            MemoTable memoTable,
            Symbol.NonTerminal rule,
            int position,
            Function<Symbol.NonTerminal, Expression> grammar,
            Heads.Head head,
            MemoTableLookup lookup,
            String name,
            Heads heads,
            Stack<Symbol.NonTerminal> ruleInvocationStack
    ) {
        if (lookup instanceof MemoTableLookup.NoHit && !head.associated(name)) {
            return new MemoTableLookup.PreviousParsingFailure();
        } else if (head.inEvaluationSet(name)) {
            head.popEval(name);
            Either<ConsumedExpression, String> consumedExpressionStringEither = evaluateNonterminal(
                    rule,
                    input,
                    position,
                    grammar,
                    memoTable,
                    heads,
                    ruleInvocationStack
            );
            if (consumedExpressionStringEither instanceof Either.This<ConsumedExpression, String>(
                    var consumedExpression
            )) {
                return new MemoTableLookup.Success(consumedExpression.parsePosition());
            } else {
                return new MemoTableLookup.PreviousParsingFailure();
            }
        } else {
            return lookup;
        }
    }

    private static Either<ConsumedExpression, String> tryParseNonterminal(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal,
            Heads heads,
            Stack<Symbol.NonTerminal> ruleInvocationStack
    ) {
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        // memoTable.setLeftRecursionDetected(key, false);
        Either<ConsumedExpression, String> applied = applyNonterminalWithoutLookup(
                input, currentPosition, grammar, memoTable, nonTerminal, heads, ruleInvocationStack
        );
        boolean detected = memoTable.getLeftRecursion(key);
        if (detected && applied instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            memoTable.removeLeftRecursion(key);
            memoTable.insertSuccess(key, consumedExpression.parsePosition());
            memoTable.setInGrowLeftRecursion(key);
            applied = growLeftRecursion(
                    applied, input, currentPosition, grammar, memoTable, nonTerminal, heads, ruleInvocationStack
            );
            memoTable.removeInGrowLeftRecursion(key);
            memoTable.removeLeftRecursion(key);
        }
        return applied;
    }

    private static Either<ConsumedExpression, String> growLeftRecursion(
            Either<ConsumedExpression, String> applied,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal,
            Heads heads,
            Stack<Symbol.NonTerminal> ruleInvocationStack
    ) {
        Either<ConsumedExpression, String> evaluated = applied;
        int lastPosition = currentPosition;
        while (true) {
            Either<ConsumedExpression, String> evaluatedNonTerminal = evaluateNonterminal(
                    nonTerminal,
                    input,
                    currentPosition,
                    grammar,
                    memoTable,
                    heads,
                    ruleInvocationStack
            );
            if (evaluatedNonTerminal instanceof Either.This<ConsumedExpression, String>(
                    var consumedExpression
            )) {
                if (consumedExpression.parsePosition() <= lastPosition) {
                    return Objects.requireNonNullElse(
                            evaluated, evaluatedNonTerminal
                    );
                }
                evaluated = evaluatedNonTerminal;
                lastPosition = consumedExpression.parsePosition();
                memoTable.insertSuccess(new MemoTableKey(nonTerminal.name(), currentPosition), consumedExpression.parsePosition());
            } else {
                return evaluated;
            }
        }
    }

    private static Either<ConsumedExpression, String> evaluateNonterminal(
            Symbol.NonTerminal nonTerminal,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Heads heads,
            Stack<Symbol.NonTerminal> ruleInvocationStack
    ) {
        Expression expression = grammar.apply(nonTerminal);
        return applyRule(expression, input, currentPosition, grammar, memoTable, heads, ruleInvocationStack);
    }

    private static Either<ConsumedExpression, String> applyNonterminalWithoutLookup(
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable,
            Symbol.NonTerminal nonTerminal,
            Heads heads,
            Stack<Symbol.NonTerminal> ruleInvocationStack
    ) {
        MemoTableKey key = new MemoTableKey(nonTerminal.name(), currentPosition);
        Either<ConsumedExpression, String> consumedInput = applyRule(
                grammar.apply(nonTerminal),
                input,
                currentPosition,
                grammar,
                memoTable,
                heads,
                ruleInvocationStack
        );
        if (consumedInput instanceof Either.This<ConsumedExpression, String>(var success)) {
            memoTable.insertSuccess(key, success.parsePosition());
        } else {
            memoTable.insertFailure(key);
        }
        return consumedInput;
    }
}