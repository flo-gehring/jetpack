package de.flogehring.jetpack.util;

import java.util.Map;

public class Check {

    private Check() {

    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }

    public static <T, S> boolean hasKey(Map<T, S> map, T key) {
        return map.containsKey(key);
    }

    public static boolean checkNotNull(Object... objects) {
        boolean result = true;
        for (Object o : objects) {
            result &= o != null;
        }
        return result;
    }

    public static void requireNotEmpty(String string) {
        Check.require(string != null, "String can't be null");
        Check.require(!string.isEmpty(), "String can't be empty");
    }
}
