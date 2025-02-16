package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.grammar.Symbol;

import java.util.Stack;

public class ParsingState {

    private int maxPos;
    private Stack<Symbol.NonTerminal> callStack;
    private boolean growState;
    private final LookupTable lookup;

    private ParsingState() {
        lookup = LookupTable.of();
    }

    public MemoTableLookup lookup(MemoTableKey key) {
        return lookup.get(key);
    }

    public static ParsingState of() {
        return new ParsingState();
    }

    public LookupTable getLookup() {
        return lookup;
    }
}
