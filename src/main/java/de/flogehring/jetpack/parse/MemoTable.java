package de.flogehring.jetpack.parse;

import java.util.*;

public class MemoTable<T> {

    private final HashMap<MemoTableKey, T> lookupTable;

    private MemoTable() {
        lookupTable = new HashMap<>();
    }

    public static <T> MemoTable<T> of() {
        return new MemoTable<>();
    }

    public void insert(MemoTableKey key, T value) {
        lookupTable.put(key, value);
    }

    public MemoTableLookup<T> get(MemoTableKey key) {
        if (lookupTable.containsKey(key)) {
            return new MemoTableLookup.Hit<>(lookupTable.get(key));
        }
        return new MemoTableLookup.NoHit<>();
    }
}