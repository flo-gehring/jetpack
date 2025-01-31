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
                case MemoTableLookup.NoHit() -> consumeInput(input, currentPosition, grammar, memoTable);
                case MemoTableLookup.Success(var parsePosition) -> Either.ofThis(
                        new ConsumedExpression(parsePosition)
                );
                case MemoTableLookup.PreviousParsingFailure() -> Either.or(
                        new RuntimeException("Previous Parsing failure")
                );
            };
        }

        private Either<ConsumedExpression, RuntimeException> consumeInput(
                Input input,
                int currentPosition,
                Function<NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            Expression expansion = grammar.apply(this);
            Either<ConsumedExpression, RuntimeException> consume = expansion.consume(input, currentPosition, grammar, memoTable);
            final MemoTableKey key = new MemoTableKey(name, currentPosition);
            switch (consume) {
                case Either.This<ConsumedExpression, RuntimeException>(var consumedExpression) ->
                        memoTable.insertSuccess(
                                key, consumedExpression.parsePosition()
                        );
                case Either.Or<ConsumedExpression, RuntimeException>(var _) ->
                        memoTable.insertFailure(key);
            }
            return consume;
        }
    }
}
