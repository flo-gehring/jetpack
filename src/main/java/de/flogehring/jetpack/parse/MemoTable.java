package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.util.Check;

import java.util.HashMap;

public class MemoTable {

    private final HashMap<MemoTableKey, Integer> lookup;

    private MemoTable() {
        lookup = new HashMap<>();
    }

    public static MemoTable of() {
        return new MemoTable();
    }

    public void insertSuccess(MemoTableKey key, int offset) {
        Check.require(
                offset > 0,
                "MemoTable offset can't be smaller than 0"
        );
        lookup.put(key, offset);
    }

    public void insertFailure(MemoTableKey key) {
        lookup.put(key, -1);
    }

    public MemoTableLookup get(MemoTableKey key) {
        if (lookup.containsKey(key)) {
            int offset = lookup.get(key);
            if (offset == -1) {
                return new MemoTableLookup.Fail();
            }
            return new MemoTableLookup.Success(offset);

        }
        return new MemoTableLookup.NoHit();
    }
}
