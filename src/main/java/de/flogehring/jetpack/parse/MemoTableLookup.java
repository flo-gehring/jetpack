package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.List;

public sealed interface MemoTableLookup {

    record NoHit() implements MemoTableLookup {
    }

    record Fail(boolean growLeftRecursion) implements MemoTableLookup {
    }

    record Success(int offset, List<Node<Symbol>> parseTree, boolean growLeftRecursion) implements MemoTableLookup {
    }
}
