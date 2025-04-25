package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.MemoTable;
import de.flogehring.jetpack.datatypes.MemoTableLookup;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

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

    public <T> T resolve(Node<Symbol> node, Class<T> target) {
        if (node.getValue() instanceof Symbol.NonTerminal nonTerminal) {
            return get(nonTerminal, target).apply(node);
        }else {
            throw new RuntimeException("Expected Non-Terminal");
        }
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
