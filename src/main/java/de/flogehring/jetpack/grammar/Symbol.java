package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;

import java.util.function.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

sealed interface Symbol extends Expression {

    static Symbol terminal(String t) {
        return new Terminal(t);
    }

    record Terminal(String symbol) implements Symbol {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(Input input, int currentPosition, Function<NonTerminal, Expression> grammar) {
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
        public Either<ConsumedExpression, RuntimeException> consume(Input input, int currentPosition, Function<NonTerminal, Expression> grammar) {
            Expression expansion = grammar.apply(this);
            return expansion.consume(input, currentPosition, grammar);
        }
    }
}
