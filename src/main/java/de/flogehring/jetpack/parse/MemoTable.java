package de.flogehring.jetpack.parse;

public class MemoTable {

    private final LookupTable lookup;

    private MemoTable() {
        lookup = LookupTable.of();
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

    }


}
