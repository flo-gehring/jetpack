package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.util.Check;

public sealed interface Symbol extends Expression {

    static Symbol terminal(String s) {
        return new Terminal(s);
    }

    static Symbol.NonTerminal nonTerminal(String s) {
        return new NonTerminal(s);
    }

    record Terminal(String symbol) implements Symbol {

        public Terminal {
            Check.requireNotNull("Terminal string can't be null", symbol);
        }
    }

    record NonTerminal(String name) implements Symbol {

        public NonTerminal {
            Check.requireNotEmpty(name);
        }
    }
}
