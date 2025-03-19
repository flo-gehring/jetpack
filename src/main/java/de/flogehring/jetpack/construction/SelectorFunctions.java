package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Function;

public class SelectorFunctions {

    private SelectorFunctions() {}

    public static Function<Node<Symbol>, Symbol.NonTerminal> selectNonterminal(int position) {
        return  symbol -> selectClass(symbol, position, Symbol.NonTerminal.class);
    }


    public static Function<Node<Symbol>, String> getTerminalValue(int position) {
        return  symbol -> selectClass(symbol, position, Symbol.Terminal.class).symbol();
    }

    private  static <T> T selectClass(Node<Symbol> node, int position, Class<T> t) {
        List<Node<Symbol>> childrenOfClass = node.getChildren().stream().filter(child -> t.isInstance(child.getValue())).toList();
        Check.require(position < childrenOfClass.size(),
                MessageFormat.format("Can not select child of class {0} at position {1}, only {2} present. Node {3} ",
                        t.getName(), position, childrenOfClass.size(), node.toString())
        );
        return t.cast(childrenOfClass.get(position).getValue());
    }
}
