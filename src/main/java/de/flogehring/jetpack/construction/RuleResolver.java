package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.MemoTable;
import de.flogehring.jetpack.datatypes.MemoTableLookup;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.function.Function;

public class RuleResolver {

    private MemoTable<Rule, Function<Node<Symbol>, ?>> rules;

    public RuleResolver(MemoTable<Rule, Function<Node<Symbol>, ?>> rules) {
        this.rules = rules;
    }

    public static RuleResolver init() {
        return new RuleResolver(MemoTable.of());
    }


    public <T> void insert(String rule, Class<T> targetClass, Function<Node<Symbol>, T> function) {
        rules.insert(new Rule(rule, targetClass), function);
    }

    public <T> Function<Node<Symbol>, T> get(String rule, Class<T> target) {
        MemoTableLookup<Function<Node<Symbol>, ?>> functionMemoTableLookup = rules.get(new Rule(rule, target));
        if (functionMemoTableLookup instanceof MemoTableLookup.Hit<Function<Node<Symbol>, ?>>(var function)) {
            return function.andThen(target::cast);
        } else {
            throw new RuntimeException("Try to resolve Rule  " + rule + " for " + target.getCanonicalName() + " but not found");
        }
    }

    private record Rule(String rule, Class<?> targetType) {
    }
}
