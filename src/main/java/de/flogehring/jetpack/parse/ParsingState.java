package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.grammar.Symbol;
import lombok.Getter;
import lombok.Setter;

import java.util.Stack;

@Getter
public class ParsingState {

    @Setter
    private int maxPos;
    private final Stack<Symbol.NonTerminal> callStack;
    @Setter
    private boolean growState;
    private final MemoTable<LookupTableEntry> lookup;

    private ParsingState() {
        lookup = MemoTable.of();
        callStack = new Stack<>();
    }

    public static ParsingState of() {
        return new ParsingState();
    }
}
