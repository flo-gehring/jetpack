package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.datatypes.Tuple;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.util.*;
import java.util.function.BiFunction;
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

    public ComposedObject composed() {
        return new ComposedObject();
    }

    public IfThenElse ifThenElse() {
        return new IfThenElse();
    }

    public class IfThenElse {

        private final List<Tuple<Function<Node<Symbol>, Boolean>, Function<Node<Symbol>, T>>> cases;
        private Function<Node<Symbol>, T> elseCase;

        private IfThenElse() {
            cases = new ArrayList<>();
        }

        public IfThenElse ifThen(
                Function<Node<Symbol>, Boolean> when,
                Function<Node<Symbol>, T> then
        ) {
            cases.add(new Tuple<>(when, then));
            return this;
        }

        public Function<Node<Symbol>, T> elseCase(Function<Node<Symbol>, T> elseCase) {
            this.elseCase = elseCase;
            return build();
        }

        public Function<Node<Symbol>, T> build() {
            return node -> cases.stream().filter(
                            tuple -> tuple.left().apply(node)
                    ).findFirst().map(tuple -> tuple.right().apply(node))
                    .or(() -> elseCase != null ? Optional.of(elseCase.apply(node)) : Optional.empty())
                    .orElseThrow(() -> new RuntimeException("No Case matched for node" + node.toString()));
        }
    }

    public class ComposedObject {

        private Function<Node<Symbol>, T> delegate;

        public ComposedObjectReady from(BiFunction<RuleResolver, Node<Symbol>, T> f) {
            delegate = symbolNode -> f.apply(
                    resolver, symbolNode
            );
            return new ComposedObjectReady();
        }

        public class ComposedObjectReady {
            private ComposedObjectReady() {
            }

            public Function<Node<Symbol>, T> build() {
                return delegate;
            }
        }

    }

    public SingleNonTerminalChild expectSingleNonTerminal() {
        return new SingleNonTerminalChild();
    }

    public class SingleNonTerminalChild {

        private final Map<String, Function<Node<Symbol>, T>> possibilities;
        private  Function<Node<Symbol>, T> defaultCase;

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
            List<Node<Symbol>> nonterminalChildren = symbolNode.getChildren()
                    .stream()
                    .filter(child -> child.getValue() instanceof Symbol.NonTerminal)
                    .toList();
            Check.require(
                    nonterminalChildren.size() == 1,
                    "Expected exactly one Non-Terminal Child for symbol " + symbolNode.getValue()
            );
            Node<Symbol> child = nonterminalChildren.getFirst();
            Symbol.NonTerminal value = (Symbol.NonTerminal) child.getValue();
            return Objects.requireNonNull(
                    possibilities.get(value.name()),
                    "No RuleResolverFunction specified for " + value.name()
            ).apply(child);
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