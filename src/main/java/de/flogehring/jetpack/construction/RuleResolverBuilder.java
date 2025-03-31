package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

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
            Function<RuleResolver, Function<Node<Symbol>, T>> construct
    ) {
        ruleResolver.insert(
                nonTerminal,
                construct.apply(ruleResolver)
        );
        return this;
    }

    public <T> RuleResolverBuilder addRuleWithFunctionBuilder(
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


    public RuleResolver get() {
        return ruleResolver;
    }


}
