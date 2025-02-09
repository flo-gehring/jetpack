package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.parse.MemoTable;
import de.flogehring.jetpack.parse.MemoTableKey;
import de.flogehring.jetpack.parse.MemoTableLookup;

import java.util.function.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

sealed interface Symbol extends Expression {

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
        public Either<ConsumedExpression, RuntimeException> consume(Input input, int currentPosition, Function<NonTerminal, Expression> grammar, MemoTable memoTable) {
            MemoTableKey key = new MemoTableKey(name, currentPosition);
            MemoTableLookup memoTableLookup = memoTable.get(key);
            return switch (memoTableLookup) {
                case MemoTableLookup.NoHit() -> evaluateBody(input, currentPosition, grammar, memoTable);
                case MemoTableLookup.Success(var parsePosition) -> Either.ofThis(
                        new ConsumedExpression(parsePosition)
                );
                case MemoTableLookup.PreviousParsingFailure() -> Either.or(
                        new RuntimeException("Previous Parsing failure")
                );
                case MemoTableLookup.LeftRecursion(var result) -> switch (result) {
                    case MemoTableLookup.LeftRecursion.Result.Fail() ->
                            Either.or(new RuntimeException("Left Recursion failure"));
                    case MemoTableLookup.LeftRecursion.Result.SeedParse(var _) -> growLeftRecursion(
                            input,
                            currentPosition,
                            grammar,
                            memoTable
                    );
                };
            };
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
            memoTable.initRuleDescent(key);
            Expression expansion = grammar.apply(this);
            Either<ConsumedExpression, RuntimeException> consume = expansion.eval(input, currentPosition, grammar, memoTable);
            switch (consume) {
                case Either.This<ConsumedExpression, RuntimeException>(var consumedExpression) ->
                        memoTable.insertSuccess(
                                key, consumedExpression.parsePosition()
                        );
                case Either.Or<ConsumedExpression, RuntimeException>(var _) -> memoTable.insertFailure(key);
            }
            return consume;
        }

        private Either<ConsumedExpression, RuntimeException> growLeftRecursion(Input input, int currentPosition, Function<NonTerminal, Expression> grammar, MemoTable memoTable) {
            Either<ConsumedExpression, RuntimeException> lastSuccessFullParse = Either.or(new RuntimeException());
            while (true) {
                Either<ConsumedExpression, RuntimeException> evaluated = eval(input, currentPosition, grammar, memoTable);
                switch (evaluated) {
                    case Either.This<ConsumedExpression, RuntimeException>(var consumedExpression) -> {
                        if (consumedExpression.parsePosition() <= currentPosition) {
                            return lastSuccessFullParse;
                        }
                    }
                    case Either.Or<ConsumedExpression, RuntimeException>(var _) -> {
                        return lastSuccessFullParse;
                    }
                }
            }
        }
    }
}
