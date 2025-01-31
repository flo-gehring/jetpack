package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.util.Check;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Input {

    private final NavigableMap<Integer, String> tokens;

    private Input(Map<Integer, String> tokens) {
        this.tokens = Collections.unmodifiableNavigableMap(new TreeMap<>(tokens));
    }

    public static Input of(String input, String whitespaceRegex) {
        String[] splitInput = input.split(whitespaceRegex);
        TreeMap<Integer, String> tokens = new TreeMap<>();
        int runningLength = 0;
        for (String token : splitInput) {
            if (!token.isEmpty()) {
                tokens.put(runningLength, token);
                runningLength += token.length();
            }
        }
        return new Input(tokens);
    }

    public int length() {
        int result;
        if (tokens.isEmpty()) {
            result = 0;
        } else {
            Map.Entry<Integer, String> lastTokenAndIndex = tokens.lastEntry();
            result = lastTokenAndIndex.getKey() + lastTokenAndIndex.getValue().length();
        }
        return result;
    }

    public String getRemainingToken(int index) {
        Map.Entry<Integer, String> tokenAndStartingPosition = tokens.floorEntry(index);
        int startOfToken = tokenAndStartingPosition.getKey();
        String token = tokenAndStartingPosition.getValue();
        int offsetInToken = index - startOfToken;
        Check.require(
                offsetInToken < token.length(),
                MessageFormat.format(

                        "Out of bounds index {0} for token {1} starting at {2} with length {3}" +
                                " -> Offset {4} too large",
                        index, token,startOfToken, token.length(), offsetInToken
                )
        );
        return token.substring(offsetInToken);
    }
}
