package de.flogehring.jetpack.parse;

import java.util.HashMap;
import java.util.Objects;

public class MemoTable {

    private final LookupTable lookup;
    private final HashMap<MemoTableKey, Boolean> leftRecursion;
    private final HashMap<MemoTableKey, Boolean> inGrowLeftRecursion;

    private MemoTable() {
        lookup = LookupTable.of();
        leftRecursion = new HashMap<>();
        inGrowLeftRecursion = new HashMap<>();
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

    public void setLeftRecursionDetected(MemoTableKey key, boolean detected) {
        leftRecursion.put(key, detected);
    }

    public boolean getInGrowLeftRecursion(MemoTableKey key) {
        return inGrowLeftRecursion.getOrDefault(key, false);
    }

    public void setInGrowLeftRecursion(MemoTableKey key) {
         inGrowLeftRecursion.put(key, true);
    }

    public boolean getLeftRecursion(MemoTableKey key) {
        return Objects.requireNonNullElse(leftRecursion.get(key), false);
    }

    public void removeLeftRecursion(MemoTableKey key) {
        leftRecursion.remove(key);
    }

    public MemoTableLookup getLookupIgnoreLeftRecursion(MemoTableKey key) {
        return lookup.get(key);
    }

    public void removeInGrowLeftRecursion(MemoTableKey key) {
        inGrowLeftRecursion.remove(key);
    }
}
