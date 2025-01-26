package de.flogehring.jetpack.grammar;

 sealed interface Symbol {

  record Terminal(String symbol) implements Symbol {

  }

  record NonTerminal(String name) implements Symbol {

  }
}
