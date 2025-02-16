package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.grammar.Symbol;
import lombok.Getter;
import lombok.Setter;

import java.util.Stack;

public class ParsingState {

    @Setter
    @Getter
    private int maxPos;
    @Getter
    private Stack<Symbol.NonTerminal> callStack;
    @Getter
    @Setter
    private boolean growState;
    @Getter
    private final LookupTable lookup;

    private ParsingState() {
        lookup = LookupTable.of();
        callStack = new Stack<>();
    }

    public MemoTableLookup lookup(MemoTableKey key) {
        return lookup.get(key);
    }

    public static ParsingState of() {
        return new ParsingState();
    }
}
