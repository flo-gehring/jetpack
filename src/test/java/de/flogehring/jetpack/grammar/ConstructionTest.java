package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.construction.Constructor;
import de.flogehring.jetpack.construction.ConstructorBuilder;
import de.flogehring.jetpack.construction.ConstructorFunction;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.ConstructionTest.Expr.MathOperation.NoOp;
import de.flogehring.jetpack.grammar.ConstructionTest.Expr.MathOperation.Op;
import de.flogehring.jetpack.grammar.ConstructionTest.Expr.MathOperation.Op.MathOperator;
import de.flogehring.jetpack.parse.Grammar;
import de.flogehring.jetpack.util.Check;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

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


        sealed interface MathOperation {
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
                                    parsingLibrary.get(Symbol.nonTerminal("Sum")).construct(
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
        assertEquals(new Expr.Root(new Expr.Sum(
                new Op(
                        descentToOne,
                        MathOperator.PLUS,
                        descentToOne
                )
        )), expressionConstructor.from(parseTree));

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
            return switch (t) {
                case "+" -> MathOperator.PLUS;
                case "*" -> MathOperator.TIMES;
                case "-" -> MathOperator.MINUS;
                case "/" -> MathOperator.DIVIDED_BY;
                case "^" -> MathOperator.POWER;
                default -> throw new RuntimeException("Unexpected Terminal " + t);
            };
        } else {
            throw new RuntimeException("Expected Terminal, got Nonterminal: " + value);
        }
    }
}
