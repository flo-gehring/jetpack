package de.friendlyhedgehog.jetpack.parse;

import de.friendlyhedgehog.jetpack.datatypes.Node;
import de.friendlyhedgehog.jetpack.grammar.Symbol;
import de.friendlyhedgehog.jetpack.util.Check;

import java.util.List;

public class ParseTreeBuilder {

    public static Node<Symbol> createTree(
            List<String> nonTerminalDescent,
            List<Node<Symbol>> childrenLast
    ) {
        Check.require(!nonTerminalDescent.isEmpty(), "Cant Create Node from empty list");
        if (nonTerminalDescent.size() == 1) {
            return Node.of(Symbol.nonTerminal(nonTerminalDescent.getFirst()), childrenLast);
        } else {
            return Node.of(
                    Symbol.nonTerminal(nonTerminalDescent.getFirst()),
                    List.of(
                            createTree(nonTerminalDescent.subList(1, nonTerminalDescent.size()), childrenLast)
                    )
            );
        }
    }

    public static Node<Symbol> terminalLeaf(String terminal) {
        return Node.leaf(Symbol.terminal(terminal));
    }
}
