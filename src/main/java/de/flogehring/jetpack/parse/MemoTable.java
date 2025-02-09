package de.flogehring.jetpack.parse;

import java.util.HashMap;
import java.util.Objects;

public class MemoTable {

    private final LookupTable lookup;
    private final HashMap<MemoTableKey, Boolean> leftRecursion;

    private MemoTable() {
        lookup = LookupTable.of();
        leftRecursion = new HashMap<>();
    }

    public static MemoTable of() {
        return new MemoTable();
    }

    public void insertSuccess(MemoTableKey key, int offset) {
        lookup.insertSuccess(key, offset);
    }

    public void insertFailure(MemoTableKey key) {
        lookup.insertFailure(key);
    }

    public MemoTableLookup get(MemoTableKey key) {
        if (leftRecursion.containsKey(key)) {
            return new MemoTableLookup.LeftRecursion(leftRecursion.get(key));
        }
        return lookup.get(key);
    }



    public void setLeftRecursion(MemoTableKey key, boolean detected) {
        leftRecursion.put(key, detected);
    }

    public boolean getLeftRecursion(MemoTableKey key) {
        return Objects.requireNonNullElse(leftRecursion.get(key), false);
    }
}
