package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.Map;
import java.util.Objects;

public class Constructor<T> {

    private final Map<Symbol, ConstructorFunction<T>> parsingLibrary;

    public Constructor(Map<Symbol, ConstructorFunction<T>> parsingLibrary) {
        this.parsingLibrary = Map.copyOf(parsingLibrary);
    }

    public T from(Node<Symbol> parseTree) {
        ConstructorFunction<T> constructor = Objects.requireNonNull(parsingLibrary.get(parseTree.getValue()));
        return constructor.construct(parseTree, parsingLibrary);
    }
}
