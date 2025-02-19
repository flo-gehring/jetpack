package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.util.HashMap;
import java.util.List;

public class MemoTable {

    private final HashMap<MemoTableKey, Integer> lookupOffset;
    private final HashMap<MemoTableKey, List<Node<Symbol>>> lookupParseTree;

    private MemoTable() {
        lookupOffset = new HashMap<>();
        lookupParseTree = new HashMap<>();
    }

    public static MemoTable of() {
        return new MemoTable();
    }

    public void insertSuccess(
            MemoTableKey key,
            int offset,
            List<Node<Symbol>> parseTree
    ) {
        Check.require(
                offset > 0,
                "MemoTable offset can't be smaller than 0"
        );
        lookupOffset.put(key, offset);
        lookupParseTree.put(key, parseTree);
    }

    public void insertFailure(MemoTableKey key) {
        lookupOffset.put(key, -1);
    }

    public MemoTableLookup get(MemoTableKey key) {
        if (lookupOffset.containsKey(key)) {
            int offset = lookupOffset.get(key);
            if (offset == -1) {
                return new MemoTableLookup.Fail();
            }
            List<Node<Symbol>> parseTree = lookupParseTree.get(key);
            return new MemoTableLookup.Success(offset, parseTree);

        }
        return new MemoTableLookup.NoHit();
    }
}
