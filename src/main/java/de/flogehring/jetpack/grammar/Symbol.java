package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.parse.MemoTable;
import de.flogehring.jetpack.parse.MemoTableKey;
import de.flogehring.jetpack.parse.MemoTableLookup;

import java.util.function.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface Symbol extends Expression {

    static Symbol terminal(String t) {
        return new Terminal(t);
    }

    record Terminal(String symbol) implements Symbol {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(Input input, int currentPosition, Function<NonTerminal, Expression> grammar, MemoTable memoTable) {
            Pattern pattern = Pattern.compile(symbol, Pattern.CASE_INSENSITIVE);
            if (currentPosition >= input.length()) {
                return Either.or(new RuntimeException("Out of tokens"));
            } else {
                String remainingToken = input.getRemainingToken(currentPosition);
                return matchString(currentPosition, pattern, remainingToken);
            }
        }

        private Either<ConsumedExpression, RuntimeException> matchString(int currentPosition, Pattern pattern, String remainingToken) {
            Matcher matcher = pattern.matcher(remainingToken);

            if (matcher.find() && matcher.start() == 0) {
                int offset = matcher.end();
                return Either.ofThis(new ConsumedExpression(currentPosition + offset));
            } else {
                return Either.or(new RuntimeException("Terminal: \"" + symbol + "\" did not match"));
            }
        }
    }

    record NonTerminal(String name) implements Symbol {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(
                Input input,
                int currentPosition,
                Function<NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            MemoTableKey key = new MemoTableKey(name, currentPosition);
            MemoTableLookup memoTableLookup = memoTable.get(key);
            System.out.println("Lookup" + key + " " + memoTableLookup);
            Either<ConsumedExpression, RuntimeException> result = switch (memoTableLookup) {
                case MemoTableLookup.NoHit() -> {
                    memoTable.initRuleDescent(key);
                    if (memoTable.alreadyVisited(key)) {
                        System.out.println("Already visited" + key);
                        memoTable.setLeftRecursion(key);
                    }
                    yield evaluateBody(input, currentPosition, grammar, memoTable);
                }
                case MemoTableLookup.Success(var parsePosition) -> Either.ofThis(
                        new ConsumedExpression(parsePosition)
                );
                case MemoTableLookup.PreviousParsingFailure() -> {
                    memoTable.setLeftRecursion(key);
                    yield Either.or(
                            new RuntimeException("Previous Parsing failure")
                    );
                }
            };
            if (memoTable.getLeftRecursion(key)
                    && result instanceof Either.This<ConsumedExpression, RuntimeException>(
                    var seedParse
            )) {
                result = Either.ofThis(growLeftRecursion(
                        seedParse,
                        input,
                        grammar,
                        memoTable
                ));
            }
            return result;
        }

        @Override
        public Either<ConsumedExpression, RuntimeException> eval(Input input, int currentPosition, Function<NonTerminal, Expression> grammar, MemoTable memoTable) {
            return evaluateBody(
                    input,
                    currentPosition,
                    grammar,
                    memoTable
            );
        }

        private Either<ConsumedExpression, RuntimeException> evaluateBody(
                Input input,
                int currentPosition,
                Function<NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            final MemoTableKey key = new MemoTableKey(name, currentPosition);
            System.out.println("Called: " + this.name + " at position " + currentPosition +
                    " already visited " + memoTable.alreadyVisited(key) +
                    " lr " + memoTable.getLeftRecursion(key));
            Expression expansion = grammar.apply(this);
            Either<ConsumedExpression, RuntimeException> consume = expansion.consume(input, currentPosition, grammar, memoTable);
            switch (consume) {
                case Either.This<ConsumedExpression, RuntimeException>(var consumedExpression) ->
                        memoTable.insertSuccess(
                                key, consumedExpression.parsePosition()
                        );
                case Either.Or<ConsumedExpression, RuntimeException>(var _) -> memoTable.insertFailure(key);
            }
            return consume;
        }

        private Either<ConsumedExpression, RuntimeException> evalWithoutLookup(
                Input input,
                int currentPosition,
                Function<NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            Expression expression = grammar.apply(this);
            return switch (expression) {
                case Operator operator -> {
                    switch (operator) {
                        case Operator.OrderedChoice choice -> evalOrderedChoice(
                                choice,
                                input,
                                currentPosition,
                                grammar,
                                memoTable
                        );
                        default -> operator.consume(input, currentPosition, grammar, memoTable);
                    }
                }
                default -> expression.consume(input, currentPosition, grammar, memoTable);
            };
        }

        private void evalOrderedChoice(Operator.OrderedChoice choice, Input input, int currentPosition, Function<NonTerminal, Expression> grammar, MemoTable memoTable) {
            choice.either().eval(input, currentPosition, grammar, memoTable);
        }

        private ConsumedExpression growLeftRecursion(ConsumedExpression seedParse, Input input, Function<NonTerminal, Expression> grammar, MemoTable memoTable) {
            System.out.println(">>> Grow left recoursion called");
            int currentPosition = seedParse.parsePosition();
            ConsumedExpression lastSuccessFullParse = seedParse;
            while (true) {
                Either<ConsumedExpression, RuntimeException> evaluated = evaluateBody(
                        input,
                        seedParse.parsePosition(),
                        grammar,
                        memoTable
                );
                switch (evaluated) {
                    case Either.This<ConsumedExpression, RuntimeException>(var nextStep) -> {
                        if (nextStep.parsePosition() <= currentPosition) {
                            System.out.println("<<< Grow left recursion exited, no progress");
                            return nextStep;
                        }
                        System.out.println("-> Growing lr" + nextStep);
                        memoTable.insertSuccess(new MemoTableKey(name(), seedParse.parsePosition()), nextStep.parsePosition());
                        lastSuccessFullParse = nextStep;
                    }
                    case Either.Or<ConsumedExpression, RuntimeException>(var _) -> {
                        System.out.println("<<< Grow left recursion exited, failure");
                        return lastSuccessFullParse;
                    }
                }
            }
        }
    }
}
