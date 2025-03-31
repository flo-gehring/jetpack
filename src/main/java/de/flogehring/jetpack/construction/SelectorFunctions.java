package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SelectorFunctions {

    private SelectorFunctions() {
    }

    public static Function<Node<Symbol>, Symbol.NonTerminal> selectNonterminal(int position) {
        return symbol -> selectClass(symbol, position, Symbol.NonTerminal.class);
    }

    public static Function<Node<Symbol>, String> getTerminalValue(int position) {
        return symbol -> selectClass(symbol, position, Symbol.Terminal.class).symbol();
    }

    private static <T> T selectClass(Node<Symbol> node, int position, Class<T> t) {
        List<Node<Symbol>> childrenOfClass = node.getChildren().stream().filter(child -> t.isInstance(child.getValue())).toList();
        Check.require(position < childrenOfClass.size(),
                MessageFormat.format("Can not select child of class {0} at position {1}, only {2} present. Node {3} ",
                        t.getName(), position, childrenOfClass.size(), node.toString())
        );
        return t.cast(childrenOfClass.get(position).getValue());
    }

    public static Function<Node<Symbol>, Symbol.NonTerminal> selectNonterminalAbsolute(int position) {
        return symbol -> selectClassAbolute(symbol, position, Symbol.NonTerminal.class);
    }

    public static Function<Node<Symbol>, String> getTerminalValueAbsolute(int position) {
        return symbol -> selectClassAbolute(symbol, position, Symbol.Terminal.class).symbol();
    }

    private static <T> T selectClassAbolute(Node<Symbol> node, int position, Class<T> t) {
        List<Node<Symbol>> children = node.getChildren();
        Check.require(position < children.size(),
                MessageFormat.format("Can not select child  {0} at position {1}, only {2} present. Node {3} ",
                        t.getName(), position, children.size(), node.toString())
        );
        return t.cast(children.get(position).getValue());
    }

    public static Function<Node<Symbol>, Boolean> isSingleNonTerminal() {
        return node -> node.getChildren().stream().map(Node::getValue)
                .filter(Symbol.NonTerminal.class::isInstance)
                .count() == 1;
    }

    public static <T> Function<Node<Symbol>, T> findChildAndApply(
            RuleResolver resolver,
            Symbol.NonTerminal node,
            Class<T> target
    ) {
        Function<Node<Symbol>, Node<Symbol>> findChild = arg -> {
            List<Node<Symbol>> list = arg.getChildren().stream().filter(child -> child.getValue().equals(node)).toList();
            Check.require(list.size() == 1, "Expected exactly one child with " + node + " but found " + list.size());
            return list.getFirst();
        };
        return findChild.andThen(n -> resolver.get(node, target).apply(n));
    }

    public static <T> Function<Node<Symbol>, T> findChildAndApply(
            RuleResolver resolver,
            Symbol.NonTerminal node,
            int position,
            Class<T> target
    ) {
        Function<Node<Symbol>, Node<Symbol>> findChild = arg -> {
            List<Node<Symbol>> list = arg.getChildren().stream().filter(child -> child.getValue().equals(node)).toList();
            Check.require(position < list.size(), "Expected at most " + (position + 1) + " + nodes " + node + "but found" + list.size());
            return list.get(position);
        };
        return findChild.andThen(n -> resolver.get(node, target).apply(n));
    }

    public static <T> Function<Node<Symbol>, Optional<T>> findOptionalAndApply(
            RuleResolver resolver,
            Symbol.NonTerminal node,
            int position, Class<T> type
    ) {
        Function<Node<Symbol>, Optional<Node<Symbol>>> findChild = arg -> {
            List<Node<Symbol>> list = arg.getChildren().stream().filter(child -> child.getValue().equals(node)).toList();
            if (position < list.size()) {
                return Optional.of(list.get(position));
            } else {
                return Optional.empty();
            }
        };
        return findChild
                .andThen(n -> n.map(found
                        -> resolver.get(node, type).apply(found)));
    }

    public static <T> Function<Node<Symbol>, List<T>> findListAndApply(
            RuleResolver resolver,
            Symbol.NonTerminal node,
            Class<T> target
    ) {
        Function<Node<Symbol>, List<Node<Symbol>>> findChild = arg ->
                arg.getChildren().stream().filter(child -> child.getValue().equals(node)).toList();
        return findChild.andThen(n -> n.stream()
                .map(found -> resolver.get(node, target).apply(found))
                .toList()
        );
    }

    // TODO Something like flatmap
    public static <T> Function<Node<Symbol>, Optional<T>> findOptionalAndApply(
            RuleResolver resolver,
            Symbol node,
            Class<T> target
    ) {
        Function<Node<Symbol>, Optional<Node<Symbol>>> findChild = arg -> {
            List<Node<Symbol>> list = arg.getChildren().stream().filter(child -> child.getValue().equals(node)).toList();
            Check.require(list.size() <= 1, "Expected at most one child with " + node + " but found " + list.size());
            return list.stream().findAny();
        };
        return findChild.andThen(
                n -> n
                        .map(found -> resolver.get((Symbol.NonTerminal) found.getValue(), target).apply(found))
        );
    }
}
