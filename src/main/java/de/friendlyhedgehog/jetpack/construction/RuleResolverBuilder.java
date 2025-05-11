package de.friendlyhedgehog.jetpack.construction;

import de.friendlyhedgehog.jetpack.datatypes.Node;
import de.friendlyhedgehog.jetpack.grammar.Symbol;

import java.util.function.Function;

public class RuleResolverBuilder {

    private final RuleResolver ruleResolver;

    private RuleResolverBuilder() {
        ruleResolver = RuleResolver.init();
    }

    public static RuleResolverBuilder init() {
        return new RuleResolverBuilder();
    }

    public <T> RuleResolverBuilder addRule(
            Symbol.NonTerminal nonTerminal,
            Class<T> target,
            Function<ResolverFunctionBuilder<T>, Function<Node<Symbol>, T>> construct
    ) {
        ruleResolver.insert(
                nonTerminal,
                construct.apply(ResolverFunctionBuilder.init(target, ruleResolver))
        );
        return this;
    }

    public RuleResolver build() {
        return ruleResolver;
    }
}
