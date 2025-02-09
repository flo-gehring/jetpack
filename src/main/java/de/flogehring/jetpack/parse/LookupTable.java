package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.util.Check;

import java.util.HashMap;

public class LookupTable {

    private final HashMap<MemoTableKey, Integer> lookup;
    private final HashMap<MemoTableKey, Boolean> leftRecursion;

    private LookupTable() {
        lookup = new HashMap<>();
        leftRecursion = new HashMap<>();
    }

    public static LookupTable of() {
        return new LookupTable();
    }

    public void insertSuccess(MemoTableKey key, int offset) {
        Check.require(
                offset > 0,
                "MemoTable offset can't be smaller than 0"
        );
        lookup.put(key, offset);
    }

    public void initRuleDescent(MemoTableKey key) {
        lookup.put(key, -1);
        leftRecursion.put(key, false);
    }

    public void insertFailure(MemoTableKey key) {
        lookup.put(key, -1);
    }

    public void setLeftRecursion(MemoTableKey key) {
        leftRecursion.put(key, true);
    }

    public MemoTableLookup get(MemoTableKey key) {
        if (leftRecursion.containsKey(key)) {
            return new MemoTableLookup.LeftRecursion();
        } else if (lookup.containsKey(key)) {
            int offset = lookup.get(key);
            if (offset == -1) {
                return new MemoTableLookup.PreviousParsingFailure();
            }
            return new MemoTableLookup.Success(offset);

        }
        return new MemoTableLookup.NoHit();
    }
}
