package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.List;

sealed interface ParsingStateLookup {

    boolean growLr();

    record Fail(boolean growLr) implements ParsingStateLookup {
    }

    record MisMatch(boolean growLr) implements ParsingStateLookup {

    }

    record Match(int parsePosition, List<Node<Symbol>> parseTree, boolean growLr) implements ParsingStateLookup {

    }
}