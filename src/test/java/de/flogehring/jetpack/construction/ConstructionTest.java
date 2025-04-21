package de.flogehring.jetpack.construction;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.construction.ConstructionTest.Expr.MathOperation.NoOp;
import de.flogehring.jetpack.construction.ConstructionTest.Expr.MathOperation.Op;
import de.flogehring.jetpack.construction.ConstructionTest.Expr.MathOperation.Op.MathOperator;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.parse.Grammar;
import de.flogehring.jetpack.util.Check;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static de.flogehring.jetpack.grammar.Symbol.nonTerminal;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstructionTest {

    sealed interface Expr {

        record Root(Expr e) implements Expr {
        }

        record Sum(MathOperation mathOperation) implements Expr {

        }

        record Product(MathOperation mathOperation) implements Expr {
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

    Constructor<Expr> expressionConstructor = ConstructorBuilder.<Expr>empty()
            .withNonterminal(
                    "Expr",
                    (symbol, parsingLibrary) ->
                            new Expr.Root(
                                    parsingLibrary.get(nonTerminal("Sum")).construct(
                                            symbol.getChildren().getFirst(),
                                            parsingLibrary
                                    )
                            )
            ).withNonterminal(
                    "Product",
                    ifSingletonDelegateElseMathOp(Expr.Product::new)
            ).withNonterminal(
                    "Sum",
                    ifSingletonDelegateElseMathOp(Expr.Sum::new)
            ).withNonterminal(
                    "Power",
                    ifSingletonDelegateElseMathOp(Expr.Power::new)
            )
            .withNonterminal("Value",
                    valueRule()
            )
            .build();

    @Test
    void testSimpleExpression() {
        Grammar testGrammar = Grammar.of(grammar).getEither();
        Node<Symbol> parseTree = testGrammar.parse("1+1").getEither();
        Expr.Product descentToOne = new Expr.Product(
                new NoOp(new Expr.Power(new NoOp(new Expr.Value(1))))
        );
        Expr.Root expected = new Expr.Root(new Expr.Sum(
                new Op(
                        descentToOne,
                        MathOperator.PLUS,
                        descentToOne
                )
        ));
        assertEquals(expected, expressionConstructor.from(parseTree));
    }

    @Test
    void testSimpleWithBuilder() {
        Grammar testGrammar = Grammar.of(grammar).getEither();
        Node<Symbol> parseTree = testGrammar.parse("1+1").getEither();
        RuleResolver resolver = RuleResolverBuilder.init()
                .addRuleWithFunctionBuilder(
                        nonTerminal("Expr"), Expr.class,
                        b -> b.expectSingleNonTerminal()
                                .delegateToResolver()
                                .andThen(Expr.Root::new)
                )
                .addRuleWithFunctionBuilder(
                        nonTerminal("Sum"),
                        Expr.class,
                        builder -> getRule(builder, "Product")
                )
                .addRuleWithFunctionBuilder(
                        nonTerminal("Product"),
                        Expr.class,
                        builder -> getRule(builder, "Power")
                )
                .addRuleWithFunctionBuilder(
                        nonTerminal("Power"),
                        Expr.class,
                        ConstructionTest::getPowerResolver)
                .addRuleWithFunctionBuilder(
                        nonTerminal("Value"),
                        Expr.class,
                        ConstructionTest::getValueResolver
                )
                .get();
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
                .ifThen(SelectorFunctions.isSingleNonTerminal(), builder.expectSingleNonTerminal().delegateToResolver())
                .elseCase(node -> new Expr.Value(
                        Integer.parseInt(((Symbol.Terminal) node.getChildren().getFirst().getValue()).symbol()))
                );
    }

    private static Function<Node<Symbol>, Expr> getPowerResolver(ResolverFunctionBuilder<Expr> builder) {
        return builder.ifThenElse()
                .ifThen(SelectorFunctions.isSingleNonTerminal(), builder.expectSingleNonTerminal().delegateToResolver())
                .elseCase(builder.composed()
                        .from((resolver1, symbolNode) ->
                                new Expr.Power(new Op(
                                        SelectorFunctions.findChildAndApply(
                                                resolver1, nonTerminal("Value"), Expr.class
                                        ).apply(symbolNode),
                                        MathOperator.POWER,
                                        SelectorFunctions.findChildAndApply(
                                                resolver1, nonTerminal("Power"), Expr.class
                                        ).apply(symbolNode)
                                )))
                        .build());
    }

    private Function<Node<Symbol>, Expr> getRule(ResolverFunctionBuilder<Expr> builder, String rule) {
        return builder.ifThenElse()
                .ifThen(SelectorFunctions.isSingleNonTerminal(),
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
                    getMathOperator(SelectorFunctions.getTerminalValueAbsolute(i).apply(symbolNode)),
                    SelectorFunctions.findChildAndApply(
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


    private ConstructorFunction<Expr> valueRule() {
        return (symbol, parsingLibrary) -> {
            if (symbol.getChildren().size() == 1) {
                Symbol value = symbol.getChildren().getFirst().getValue();
                if (value instanceof Symbol.Terminal(var t)) {
                    return new Expr.Value(Integer.parseInt(t));
                } else {
                    throw new RuntimeException();
                }
            } else {
                Node<Symbol> second = symbol.getChildren().get(1);
                return new Expr.Root(
                        parsingLibrary.get(second.getValue()).construct(
                                second,
                                parsingLibrary
                        )
                );
            }
        };
    }

    private ConstructorFunction<Expr> ifSingletonDelegateElseMathOp(
            Function<Expr.MathOperation, Expr> constructor
    ) {

        return (symbol, parsingLibrary) -> {
            List<Node<Symbol>> children = symbol.getChildren();
            int numChildren = children.size();
            Node<Symbol> firstChild = children.getFirst();
            if (numChildren == 1) {
                return constructor.apply(new NoOp(
                        parsingLibrary.get(firstChild.getValue()).construct(firstChild, parsingLibrary)
                ));
            } else {
                Check.require(numChildren == 3, "Expected exactly 3 Children");
                return constructor.apply(
                        new Op(
                                parsingLibrary.get(firstChild.getValue()).construct(
                                        firstChild, parsingLibrary

                                ),
                                mapOp(children.get(1).getValue()),
                                parsingLibrary.get(firstChild.getValue()).construct(
                                        firstChild, parsingLibrary

                                )
                        )
                );
            }
        };
    }

    private MathOperator mapOp(Symbol value) {
        if (value instanceof Symbol.Terminal(String t)) {
            return getMathOperator(t);
        } else {
            throw new RuntimeException("Expected Terminal, got Nonterminal: " + value);
        }
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