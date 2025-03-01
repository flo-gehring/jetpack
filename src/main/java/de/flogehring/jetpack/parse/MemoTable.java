package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.*;

public class MemoTable<T> {

    private final HashMap<MemoTableKey, T> lookupTable;

    private MemoTable() {
        lookupTable = new HashMap<>();
    }

    public static <T> MemoTable<T> of() {
        return new MemoTable<>();
    }

    public void insert(
            MemoTableKey key,
            T value
    ) {

        lookupTable.put(key, value);
    }

    public void insertFailure(MemoTableKey key, boolean growLr) {
        lookupTable.put(key, new LookupTableEntry.Fail(growLr));
    }

    public MemoTableLookup<T> get(MemoTableKey key) {
        if (lookupTable.containsKey(key)) {
            return new MemoTableLookup.Hit<>(lookupTable.get(key));
        }
        return new MemoTableLookup.NoHit<>();
    }

    public Optional<MemoTableKey> getHighestSuccess() {
        // TODO
        return Optional.empty();
    }
}
