package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.MemoTable;
import de.flogehring.jetpack.datatypes.MemoTableLookup;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RuleResolver {

    private final MemoTable<Symbol.NonTerminal, Function<Node<Symbol>, ?>> rules;

    private RuleResolver(MemoTable<Symbol.NonTerminal, Function<Node<Symbol>, ?>> rules) {
        this.rules = rules;
    }

    public static RuleResolver init() {
        return new RuleResolver(MemoTable.of());
    }

    public <T> void insert(Symbol.NonTerminal nonTerminal, Function<Node<Symbol>, T> function) {
        rules.insert(nonTerminal, function);
    }

    public <T> Function<Node<Symbol>, T> findChildAndApply(
            Symbol.NonTerminal node,
            Class<T> target
    ) {
        Function<Node<Symbol>, Node<Symbol>> findChild = arg -> {
            List<Node<Symbol>> list = arg.getChildren().stream().filter(child -> child.getValue().equals(node)).toList();
            Check.require(list.size() == 1, "Expected exactly one child with " + node + " but found " + list.size());
            return list.getFirst();
        };
        return findChild.andThen(n -> get(node, target).apply(n));
    }

    public <T> Function<Node<Symbol>, T> findChildAndApply(
            Symbol.NonTerminal node,
            int position,
            Class<T> target
    ) {
        Function<Node<Symbol>, Node<Symbol>> findChild = arg -> {
            List<Node<Symbol>> list = arg.getChildren().stream().filter(child -> child.getValue().equals(node)).toList();
            Check.require(position < list.size(), "Expected at most " + (position + 1) + " + nodes " + node + "but found" + list.size());
            return list.get(position);
        };
        return findChild.andThen(n -> get(node, target).apply(n));
    }

    public <T> Function<Node<Symbol>, Optional<T>> findOptionalAndApply(
            Symbol.NonTerminal node, int position, Class<T> type
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
                        -> get(node, type).apply(found)));
    }


    public <T> Function<Node<Symbol>, List<T>> findListAndApply(
            Symbol.NonTerminal node,
            Class<T> target
    ) {
        Function<Node<Symbol>, List<Node<Symbol>>> findChild = arg ->
                arg.getChildren().stream().filter(child -> child.getValue().equals(node)).toList();
        return findChild.andThen(
                n -> n.stream()
                        .map(found -> get(node, target).apply(found)).toList()
        );
    }
    // TODO Something like flatmap

    public <T> Function<Node<Symbol>, Optional<T>> findOptionalAndApply(
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
                        .map(found -> get((Symbol.NonTerminal) found.getValue(), target).apply(found))
        );
    }

    public <T> Function<Node<Symbol>, T> get(Symbol.NonTerminal nonTerminal, Class<T> target) {
        MemoTableLookup<Function<Node<Symbol>, ?>> ruleLookup = rules.get(nonTerminal);
        if (ruleLookup instanceof MemoTableLookup.Hit<Function<Node<Symbol>, ?>>(var function)) {
            return function.andThen(target::cast);
        } else {
            throw new RuntimeException("Try to resolve Rule  " + nonTerminal + " for " + target.getCanonicalName() + " but not found");
        }
    }
}
