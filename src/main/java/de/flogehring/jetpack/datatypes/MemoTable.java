package de.flogehring.jetpack.datatypes;

import java.util.*;

public class MemoTable<S, T> {

    private final HashMap<S, T> lookupTable;

    private MemoTable() {
        lookupTable = new HashMap<>();
    }

    public static <S, T> MemoTable<S, T> of() {
        return new MemoTable<>();
    }

    public void insert(S key, T value) {
        lookupTable.put(key, value);
    }

    public MemoTableLookup<T> get(S key) {
        if (lookupTable.containsKey(key)) {
            return new MemoTableLookup.Hit<>(lookupTable.get(key));
        }
        return new MemoTableLookup.NoHit<>();
    }
}