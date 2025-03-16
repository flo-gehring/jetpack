package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class ResolverFunctionBuilder<T> {


    private final RuleResolver resolver;
    private final Class<T> target;

    private ResolverFunctionBuilder(RuleResolver resolver, Class<T> target) {
        this.resolver = resolver;
        this.target = target;
    }

    public static <T> ResolverFunctionBuilder<T> init(Class<T> target, RuleResolver resolver) {
        return new ResolverFunctionBuilder<>(resolver, target);
    }

    public SingleNonTerminalChild expectSingleNonTerminal() {
        return new SingleNonTerminalChild();
    }

    public class SingleNonTerminalChild {

        Map<String, Function<Node<Symbol>, T>> possibilities;

        private SingleNonTerminalChild() {
            possibilities = new HashMap<>();
        }

        public OnRule onRule(String s) {
            return new OnRule(this, s);
        }

        public Function<Node<Symbol>, T> build() {
            return this::resolve;
        }

        private T resolve(Node<Symbol> symbolNode) {
            Check.require(
                    symbolNode.getChildren().size() == 1,
                    "Expected Exactly one Child for symbol " + symbolNode.getValue()
            );
            Node<Symbol> child = symbolNode.getChildren().getFirst();
            Symbol value = child.getValue();
            if (value instanceof Symbol.NonTerminal(var name)) {
                return Objects.requireNonNull(
                        possibilities.get(name),
                        "No RuleResolverFunction specified for " + name
                ).apply(child);
            } else {
                throw new RuntimeException("Expected Non-Terminal Child, found Terminal");
            }
        }

        public class OnRule {

            private final SingleNonTerminalChild caller;
            private final String rule;

            private OnRule(SingleNonTerminalChild caller, String rule) {
                this.caller = caller;
                this.rule = rule;
            }

            public SingleNonTerminalChild delegateToResolver() {
                possibilities.put(rule, symbolNode -> resolver.get(rule, target).apply(symbolNode));
                return caller;
            }

        }
    }

}
