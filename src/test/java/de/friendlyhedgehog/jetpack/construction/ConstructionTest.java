package de.friendlyhedgehog.jetpack.construction;

import de.friendlyhedgehog.jetpack.construction.ConstructionTest.Expr.MathOperation.NoOp;
import de.friendlyhedgehog.jetpack.construction.ConstructionTest.Expr.MathOperation.Op;
import de.friendlyhedgehog.jetpack.construction.ConstructionTest.Expr.MathOperation.Op.MathOperator;
import de.friendlyhedgehog.jetpack.datatypes.Node;
import de.friendlyhedgehog.jetpack.grammar.Symbol;
import de.friendlyhedgehog.jetpack.parse.Grammar;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static de.friendlyhedgehog.jetpack.grammar.Symbol.nonTerminal;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstructionTest {

    sealed interface Expr {

        record Root(Expr e) implements Expr {
        }

        record Sum(MathOperation mathOperation) implements Expr {

        }

        record Power(MathOperation mathOperation) implements Expr {
        }

        record Value(int value) implements Expr {
        }


        sealed interface MathOperation extends Expr {
            record Op(Expr lhs, MathOperator op, Expr rhs) implements MathOperation {
                public enum MathOperator {
                    PLUS,
                    MINUS,
                    TIMES,
                    DIVIDED_BY,
                    POWER
                }
            }

            record NoOp(Expr expr) implements MathOperation {
            }
        }
    }

    private static final String grammar = """
              Expr <- Sum
                    Sum <- Product (("\\+" / "-") Product)*
                    Product <- Power (("\\*" / "/") Power)*
                    Power <- Value ("\\^" Power)?
                    Value <- "[0-9]+" / "\\(" Expr "\\)"
            """;

    @Test
    void testSimpleWithBuilder() {
        Grammar testGrammar = Grammar.of(grammar).getEither();
        Node<Symbol> parseTree = testGrammar.parse("1+1").getEither();
        RuleResolver resolver = RuleResolverBuilder.init()
                .addRule(
                        nonTerminal("Expr"), Expr.class,
                        b -> b.expectSingleNonTerminal()
                                .delegateToResolver()
                                .andThen(Expr.Root::new)
                )
                .addRule(
                        nonTerminal("Sum"),
                        Expr.class,
                        builder -> getRule(builder, "Product")
                )
                .addRule(
                        nonTerminal("Product"),
                        Expr.class,
                        builder -> getRule(builder, "Power")
                )
                .addRule(
                        nonTerminal("Power"),
                        Expr.class,
                        ConstructionTest::getPowerResolver)
                .addRule(
                        nonTerminal("Value"),
                        Expr.class,
                        ConstructionTest::getValueResolver
                )
                .build();
        Expr.Root expected = new Expr.Root(new Expr.Sum(
                new Op(
                        new NoOp(new Expr.Value(1)),
                        MathOperator.PLUS,
                        new Expr.Value(1)
                )
        ));
        assertEquals(expected, resolver.resolve(parseTree, Expr.class));
    }

    private static Function<Node<Symbol>, Expr> getValueResolver(ResolverFunctionBuilder<Expr> builder) {
        return builder.ifThenElse()
                .ifThen(ResolverFunctionHelper.isSingleNonTerminal(), builder.expectSingleNonTerminal().delegateToResolver())
                .elseCase(node -> new Expr.Value(
                        Integer.parseInt(((Symbol.Terminal) node.getChildren().getFirst().getValue()).symbol()))
                );
    }

    private static Function<Node<Symbol>, Expr> getPowerResolver(ResolverFunctionBuilder<Expr> builder) {
        return builder.ifThenElse()
                .ifThen(ResolverFunctionHelper.isSingleNonTerminal(), builder.expectSingleNonTerminal().delegateToResolver())
                .elseCase(builder.composed()
                        .from((resolver1, symbolNode) ->
                                new Expr.Power(new Op(
                                        ResolverFunctionHelper.findChildAndApply(
                                                resolver1, nonTerminal("Value"), Expr.class
                                        ).apply(symbolNode),
                                        MathOperator.POWER,
                                        ResolverFunctionHelper.findChildAndApply(
                                                resolver1, nonTerminal("Power"), Expr.class
                                        ).apply(symbolNode)
                                )))
                        .build());
    }

    private Function<Node<Symbol>, Expr> getRule(ResolverFunctionBuilder<Expr> builder, String rule) {
        return builder.ifThenElse()
                .ifThen(ResolverFunctionHelper.isSingleNonTerminal(),
                        builder.expectSingleNonTerminal().delegateToResolver()
                ).elseCase(builder.composed().from((resolver1, symbolNode)
                        -> new Expr.Sum(getMathOp(resolver1, symbolNode, rule))
                ).build());
    }

    private Expr.MathOperation getMathOp(RuleResolver resolver, Node<Symbol> symbolNode, String rule) {
        Expr.MathOperation latest = new Expr.MathOperation.NoOp(resolver.resolve(
                symbolNode.getChildren().getFirst(),
                Expr.class
        ));
        int ruleCount = 1;
        for (int i = 1; i < symbolNode.getChildren().size(); i += 2) {
            latest = new Op(
                    latest,
                    getMathOperator(ResolverFunctionHelper.getTerminalValueAbsolute(i).apply(symbolNode)),
                    ResolverFunctionHelper.findChildAndApply(
                            resolver,
                            nonTerminal(rule),
                            ruleCount,
                            Expr.class
                    ).apply(symbolNode)
            );
            ruleCount++;
        }
        return latest;
    }

    private static MathOperator getMathOperator(String t) {
        return switch (t) {
            case "+" -> MathOperator.PLUS;
            case "*" -> MathOperator.TIMES;
            case "-" -> MathOperator.MINUS;
            case "/" -> MathOperator.DIVIDED_BY;
            case "^" -> MathOperator.POWER;
            default -> throw new RuntimeException("Unexpected Terminal " + t);
        };
    }
}