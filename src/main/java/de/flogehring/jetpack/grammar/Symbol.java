package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;

import java.util.function.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

sealed interface Symbol extends Expression {

    record Terminal(String symbol) implements Symbol {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<NonTerminal, Expression> grammar) {
            Pattern pattern = Pattern.compile(symbol, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(s.substring(currentPosition));

            if(matcher.matches()) {
                int offset = matcher.end();
                return Either.ofThis(new ConsumedExpression(currentPosition + offset));
            }
            else {
                return Either.or(new RuntimeException("Terminal" + symbol + " did not match"));
            }
        }
    }

    record NonTerminal(String name) implements Symbol {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<NonTerminal, Expression> grammar) {
            return null;
        }
    }
}
