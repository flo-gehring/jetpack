package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.grammar.Symbol;

import java.util.HashMap;
import java.util.Map;

public class ConstructorBuilder<T> {

    private final Map<Symbol, ConstructorFunction<T>> parsingLibrary;


    private ConstructorBuilder() {
        this.parsingLibrary = new HashMap<>();
    }

    public static <T> ConstructorBuilder<T> empty() {
        return new ConstructorBuilder<>();
    }

    public ConstructorBuilder<T> withNonterminal(
            String nonterminal,
            ConstructorFunction<T> t
    ) {
        parsingLibrary.put(Symbol.nonTerminal(nonterminal), t);
        return this;
    }

    public Constructor<T> build() {
        return new Constructor<>(parsingLibrary);
    }
}
