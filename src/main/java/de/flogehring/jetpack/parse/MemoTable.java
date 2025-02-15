package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.grammar.ConsumedExpression;
import de.flogehring.jetpack.grammar.Operator;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class MemoTable {

    private final LookupTable lookup;
    private final HashMap<MemoTableKey, MemoTableLookup.LeftRecursion> leftRecursion;
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
            return leftRecursion.get(key);
        }
        return lookup.get(key);
    }

    public void setLeftRecursionDetected(
            MemoTableKey key,
            ConsumedExpression seed,
            Symbol.NonTerminal rule,
            Heads.Head head,
            Optional<MemoTableLookup.LeftRecursion> next
            ) {
        leftRecursion.put(key, new MemoTableLookup.LeftRecursion(
                seed,
                rule,
                head,
                next
        ));
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
