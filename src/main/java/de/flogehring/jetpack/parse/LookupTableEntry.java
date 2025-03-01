package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.List;

public sealed interface LookupTableEntry {

    record Fail(boolean growLr) implements LookupTableEntry {

    }

    record Success(int parsePosition, List<Node<Symbol>> parseTree, boolean growLr) implements LookupTableEntry {

    }

}
