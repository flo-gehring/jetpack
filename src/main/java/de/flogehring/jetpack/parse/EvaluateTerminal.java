package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.ConsumedExpression;
import de.flogehring.jetpack.grammar.Input;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvaluateTerminal {


    public static Either<ConsumedExpression, String> applyTerminal(String symbol, Input input, int currentPosition) {
        Pattern pattern = Pattern.compile(symbol, Pattern.CASE_INSENSITIVE);
        if (currentPosition >= input.length()) {
            return Either.or("Out of tokens");
        } else {
            String remainingToken = input.getRemainingToken(currentPosition);
            return matchString(currentPosition, pattern, remainingToken);
        }
    }

    private static Either<ConsumedExpression, String> matchString(int currentPosition, Pattern pattern, String remainingToken) {
        Matcher matcher = pattern.matcher(remainingToken);
        if (matcher.find() && matcher.start() == 0) {
            int offset = matcher.end();
            return Either.ofThis(new ConsumedExpression(currentPosition + offset));
        } else {
            return Either.or("Terminal: \"" + pattern.pattern() + "\" did not match");
        }
    }
}
