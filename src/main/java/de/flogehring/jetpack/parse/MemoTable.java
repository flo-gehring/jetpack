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
        return lookup.get(key);
    }

    public void initRuleDescent(MemoTableKey key) {
        lookup.initRuleDescent(key);
        leftRecursion.putIfAbsent(key, false);
    }

    public boolean alreadyVisited(MemoTableKey key) {
        return leftRecursion.containsKey(key);
    }

    public void setLeftRecursion(MemoTableKey key) {
        leftRecursion.put(key, true);
    }

    public boolean getLeftRecursion(MemoTableKey key) {
        return Objects.requireNonNull(leftRecursion.get(key));
    }
}
