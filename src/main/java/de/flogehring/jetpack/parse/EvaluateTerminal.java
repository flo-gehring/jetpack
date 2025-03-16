package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EvaluateTerminal {

    private EvaluateTerminal() {
    }

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
            return Either.ofThis(new ConsumedExpression(
                    currentPosition + offset,
                    List.of(Node.leaf(new Symbol.Terminal(
                            remainingToken.substring(0, offset)
                    )))));
        } else {
            return Either.or("Terminal: \"" + pattern.pattern() + "\" did not match");
        }
    }
}
