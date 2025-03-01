package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.datatypes.Tuple;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.util.*;

public class MemoTable {

    private final HashMap<MemoTableKey, Integer> lookupOffset;
    private final HashMap<MemoTableKey, List<Node<Symbol>>> lookupParseTree;

    private final HashMap<MemoTableKey, LookupTableEntry> lookupTable;

    private MemoTable() {
        lookupOffset = new HashMap<>();
        lookupParseTree = new HashMap<>();
        lookupTable = new HashMap<>();
    }

    public static MemoTable of() {
        return new MemoTable();
    }

    public void insertSuccess(
            MemoTableKey key,
            int offset,
            List<Node<Symbol>> parseTree,
            boolean growLr
    ) {
        Check.require(
                offset > 0,
                "MemoTable offset can't be smaller than 0"
        );
        lookupOffset.put(key, offset);
        lookupParseTree.put(key, parseTree);
        lookupTable.put(key, new LookupTableEntry.Success(
                offset, parseTree, growLr
        ));
    }

    public void insertFailure(MemoTableKey key, boolean growLr) {
        lookupOffset.put(key, -1);
        lookupTable.put(key, new LookupTableEntry.Fail(growLr));
    }

    public MemoTableLookup get(MemoTableKey key) {
        if (lookupTable.containsKey(key)) {
            return switch (lookupTable.get(key)) {
                case LookupTableEntry.Success(var offset, var parseTree, var growLr) ->
                    new MemoTableLookup.Success(offset, parseTree, growLr);
                case LookupTableEntry.Fail(var growLr) -> new MemoTableLookup.Fail(growLr);
            };
        }
        return new MemoTableLookup.NoHit();
    }

    public Optional<MemoTableKey> getHighestSuccess() {
        return lookupOffset.entrySet().stream()
                .filter(entry -> entry.getValue() != -1)
                .map(Map.Entry::getKey)
                .max(Comparator.comparing(MemoTableKey::position));
    }
}
