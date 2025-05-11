package de.friendlyhedgehog.jetpack.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

public class Check {

    private Check() {
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }

    public static <T> T requireSingleItem(List<T> list, String message) {
        if (list.size() != 1) {
            throw new RuntimeException(MessageFormat.format("Expected a single item, got {0}. {1}", list.size(), message));
        }
        return list.getFirst();
    }

    public static <T, S> boolean hasKey(Map<T, S> map, T key) {
        return map.containsKey(key);
    }

    public static void requireNotNull(String message, Object... objects) {
        boolean result = true;
        for (Object o : objects) {
            result &= o != null;
        }
        require(result, message);
    }

    public static void requireNotEmpty(String string) {
        Check.require(string != null, "String can't be null");
        Check.require(!string.isEmpty(), "String can't be empty");
    }
}
