package de.flogehring.jetpack.grammar;

public sealed interface Symbol extends Expression {

    static Symbol terminal(String s) {
        return new Terminal(s);
    }

    static Symbol nonTerminal(String s) {
        return new NonTerminal(s);
    }

    record Terminal(String symbol) implements Symbol {

    }

    record NonTerminal(String name) implements Symbol {
    }
}
