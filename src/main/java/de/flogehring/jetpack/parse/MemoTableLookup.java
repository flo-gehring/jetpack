package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

public sealed interface MemoTableLookup {

    record NoHit() implements MemoTableLookup {
    }

    record Fail() implements MemoTableLookup {
    }

    record Success(int offset, Node<Symbol> parseTree) implements MemoTableLookup {
    }
}
