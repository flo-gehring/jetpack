package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.List;

public sealed interface LookupTableEntry {

    boolean growLr();

    record Fail(boolean growLr) implements LookupTableEntry {

    }

    record MisMatch(boolean growLr)  implements LookupTableEntry{

    }
    record Match(int parsePosition, List<Node<Symbol>> parseTree, boolean growLr) implements LookupTableEntry {

    }

}
