package de.friendlyhedgehog.jetpack.parse;

import de.friendlyhedgehog.jetpack.util.Check;

import java.text.MessageFormat;
import java.util.*;

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
            String trimmedToken = token.trim();
            if (!trimmedToken.isEmpty()) {
                tokens.put(runningLength, trimmedToken);
                runningLength += trimmedToken.length();
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
                        index, token, startOfToken, token.length(), offsetInToken
                )
        );
        return token.substring(offsetInToken);
    }

    // TODO Test
    public String left(int position) {
        SortedMap<Integer, String> headMap = tokens.headMap(position);
        String startOfToken = getStartOfToken(position);
        return headMap.values().stream().reduce(" ", String::concat) + (startOfToken.isEmpty() ? "" : " " + startOfToken);
    }

    // TODO Test
    public String right(int position) {
        SortedMap<Integer, String> headMap = tokens.tailMap(position);
        String remainingToken = getRemainingToken(position);
        return  (remainingToken.isEmpty() ? "" : " " + remainingToken) + headMap.values().stream().reduce(" ", String::concat);
    }

    private String getStartOfToken(int position) {
        Map.Entry<Integer, String> entry = Objects.requireNonNull(tokens.floorEntry(position));
        return entry.getValue().substring(0, position - entry.getKey());
    }
}