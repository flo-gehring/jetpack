package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.Map;

public interface ConstructorFunction<T> {

    T construct(
            Node<Symbol> parseTree,
            Map<Symbol, ConstructorFunction<T>> parsingLibrary
    );
}
