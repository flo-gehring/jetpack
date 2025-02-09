package de.flogehring.jetpack.grammar;

public sealed interface Symbol extends Expression {

    record Terminal(String symbol) implements Symbol {

    }

    record NonTerminal(String name) implements Symbol {
    }
}
