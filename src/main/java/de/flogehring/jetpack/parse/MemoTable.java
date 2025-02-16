package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.grammar.ConsumedExpression;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class MemoTable {

    private final LookupTable lookup;


    private MemoTable() {
        lookup = LookupTable.of();
    }

    public MemoTableLookup lookup(MemoTableKey key) {
        return lookup.get(key);
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
}
