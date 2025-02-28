package de.flogehring.jetpack.grammar;


import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.datatypes.Node;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static de.flogehring.jetpack.grammar.ParseTreeBuilder.createTree;
import static de.flogehring.jetpack.grammar.ParseTreeBuilder.terminalLeaf;
import static org.junit.jupiter.api.Assertions.*;

public class GrammarTest {

    @Nested
    class MathGrammar {

        @Nested
        class Matches {
            @Test
            void matches() {
                assertTrue(testGrammar.fitsGrammar(
                        "10"
                ));
            }

            @ParameterizedTest
            @CsvSource(value = {
                    "One Expression,10,true",
                    "Simple Plus,10 + 10,true",
                    "Full Expression,(1)+(1),true",
                    "Unclosed Parenthesis,(1 +,false",
                    "Only Product,19 ^ 3 / 1 * 5, true",
                    "Only Product in Parenthesis,(19 ^ 3 / 1 * 5), true",
                    "Parenthesis,(1), true",
                    "(1 Plus 1),(1 +1), true",
                    "Parenthesis,2^2, true",
                    "Parenthesis,(1), true",
                    "Parenthesis,((1)), true",
                    "Parenthesis Mismatch,(1)), false",
                    "Double operators,1**1, false",
            })
            void testMatches(String testMessage, String expr, boolean expected) {
                assertEquals(expected, testGrammar.fitsGrammar(expr), testMessage);
            }
        }

        @Nested
        class Parse {

            @Test
            void simple() {
                Node<Symbol> parseTree = testGrammar.parse("10").getEither();
                Node<Symbol> expected = createTree(
                        List.of("Expr", "Sum", "Product", "Power", "Value"),
                        List.of(terminalLeaf("10"))
                );
                assertEquals(expected, parseTree);
            }

            @Test
            void onePlusOne() {
                Node<Symbol> parseTree = testGrammar.parse("(1)+(1)").getEither();
                List<String> descentFromSum = List.of("Product", "Power", "Value");
                List<String> descentFromExpr = List.of("Expr", "Sum", "Product", "Power", "Value");
                Node<Symbol> oneInParantheses = createTree(
                        descentFromSum,
                        List.of(
                                terminalLeaf("("),
                                createTree(
                                        descentFromExpr,
                                        List.of(terminalLeaf("1"))
                                ),
                                terminalLeaf(")")
                        )
                );
                Node<Symbol> expected = createTree(
                        List.of("Expr", "Sum"),
                        List.of(
                                oneInParantheses,
                                terminalLeaf("+"),
                                oneInParantheses
                        )
                );
                assertEquals(expected, parseTree);
            }
        }

        @Nested
        class CreateGrammar {
            private static final String grammarByText = """
                    Expr <- Sum
                    Sum <- Product (("\\+" / "-") Product)*
                    Product <- Power (("\\*" / "/") Power)*
                    Power <- Value ("\\^" Power)?
                    Value <- "[0-9]+" / "\\(" Expr "\\)"
                    """;

            @Test
            void createGrammar() {

                Grammar actual = Grammar.of(grammarByText).getEither();
                Map<String, Expression> actualRules = actual.getRules();
                Map<String, Expression> expectedRules = testGrammar.getRules();
                assertEquals(actualRules.keySet(), expectedRules.keySet());
                assertEquals(actual.getStartingRule(), testGrammar.getStartingRule());
                for (var key : actualRules.keySet()) {
                    assertEquals(testGrammar.getRules().get(key), actual.getRules().get(key), "Comparing rule: " + key);
                }
            }
        }

        /**
         * Expr    <-  Sum
         * Sum     <-  Product (('+' / '-') Product)*
         * Product <- Power (('*' / '/') Power)*
         * Power  <- Value ('^' Power)?
         * Value  <- [0-9]+ / '(' Expr ')'
         */
        Grammar testGrammar = new Grammar(
                "Expr",
                Map.of(
                        "Expr", Expression.nonTerminal("Sum"),
                        "Sum", Expression.sequence(
                                Expression.nonTerminal("Product"),
                                Expression.star(Expression.group(
                                        Expression.sequence(
                                                Expression.group(Expression.orderedChoice(
                                                        Expression.terminal("\\+"),
                                                        Expression.terminal("-")
                                                )),
                                                Expression.nonTerminal("Product")
                                        )
                                ))),
                        "Product", Expression.sequence(
                                Expression.nonTerminal("Power"),
                                Expression.star(Expression.group(
                                        Expression.sequence(
                                                Expression.group(Expression.orderedChoice(
                                                        Expression.terminal("\\*"),
                                                        Expression.terminal("/")
                                                )),
                                                Expression.nonTerminal("Power")
                                        )
                                ))),
                        "Power", Expression.sequence(
                                Expression.nonTerminal(
                                        "Value"
                                ),
                                Expression.optional(
                                        Expression.group(
                                                Expression.sequence(Expression.terminal("\\^"), Expression.nonTerminal("Power"))
                                        )
                                )
                        ),
                        "Value", Expression.orderedChoice(
                                Expression.terminal("[0-9]+"),
                                Expression.sequence(
                                        Expression.terminal("\\("),
                                        Expression.nonTerminal("Expr"),
                                        Expression.terminal("\\)")
                                )
                        )
                )
        );
    }

    @Nested
    class DirectLeftRecursion {

        Grammar grammar = new Grammar(
                "expr",
                Map.of(
                        "expr", Expression.orderedChoice(
                                Expression.sequence(
                                        Expression.nonTerminal(
                                                "expr"
                                        ),
                                        Expression.sequence(
                                                Expression.terminal("-"),
                                                Expression.nonTerminal("num")
                                        )
                                ),
                                Expression.nonTerminal("num")
                        ),
                        "num", Expression.terminal("[0-9]+")
                )
        );


        @Test
        @Timeout(1)
        void testDoesNotMatchTimeout() {
            boolean actual = grammar.fitsGrammar(
                    "1 -"
            );
            assertFalse(actual, "Does not Match missing num at the end");
        }

        @CsvSource(value = {
                "Single Number,1,true",
                "Two Numbers,1-1,true",
                "Three Numbers,1-1-1,true",
                "Multiple Digits,10,true",
                "Multiple Digits,10 - 101,true",
                "Multiple Different,1 - 1 -2 -3-4-1,true",
                "Does not match because expr not completed,1 - ,false",
                "Completely wrong,1 - asdf ,false"
        })
        @ParameterizedTest
        @Timeout(5)
        void test(String testMessage, String expression, boolean expected) {
            boolean actual = grammar.fitsGrammar(
                    expression
            );
            assertEquals(expected, actual, testMessage);
        }
    }

    @Nested
    class IndirectLeftRecursionTwo {

        Grammar grammar = new Grammar(
                "Expr",
                Map.of(
                        "Expr", Expression.orderedChoice(
                                Expression.sequence(
                                        Expression.nonTerminal("Expr"),
                                        Expression.sequence(
                                                Expression.terminal("\\+"),
                                                Expression.nonTerminal("Num"))
                                ),
                                Expression.nonTerminal("Num")

                        ),
                        "Num", Expression.orderedChoice(
                                Expression.sequence(
                                        Expression.nonTerminal("Num"),
                                        Expression.nonTerminal("Digit")
                                ),
                                Expression.nonTerminal("Digit")
                        ),
                        "Digit", Expression.terminal("[0-9]")
                )
        );

        @Test
        void fitsGrammar() {
            assertTrue(
                    grammar.fitsGrammar("12 + 3")
            );
        }


        @Nested
        class Parse {

            @Test
            void simple() {
                Node<Symbol> actual = grammar.parse("1+1").getEither();
                Node<Symbol> expected = createTree(
                        List.of("Expr"),
                        List.of(
                                createTree(List.of("Num", "Digit"), List.of(terminalLeaf("1"))),
                                terminalLeaf("+"),
                                createTree(List.of("Num", "Digit"), List.of(terminalLeaf("1")))
                        )
                );
                assertEquals(expected, actual);
            }

            @Test
            @Disabled
            void dalmatiansReversedParseFailure() {
                assertInstanceOf(Either.This.class, grammar.parse("1 + 100"));
            }

            @Test
            void dalmatians() {
                Node<Symbol> expected = createTree(
                        List.of("Expr"),
                        List.of(
                                createTree(
                                        List.of("Expr"),
                                        List.of(createTree(
                                                List.of("Num"),
                                                List.of(
                                                        createTree(List.of("Digit"), List.of(terminalLeaf("1"))),
                                                        createTree(List.of("Digit"), List.of(terminalLeaf("0"))),
                                                        createTree(List.of("Digit"), List.of(terminalLeaf("0")))
                                                )))
                                ),
                                terminalLeaf("+"),
                                createTree(List.of("Num", "Digit"), List.of(terminalLeaf("1")))
                        ));
                assertEquals(expected, grammar.parse("100 + 1").getEither());
            }
        }
    }

    @Nested
    class IndirectLeftRecursion {

        Grammar grammar = new Grammar(
                "x",
                Map.of(
                        "x", Expression.nonTerminal("expr"),
                        "expr", Expression.orderedChoice(
                                Expression.sequence(Expression.nonTerminal("x"), Expression.sequence(Expression.terminal("-"), Expression.nonTerminal("num"))
                                ),
                                Expression.nonTerminal("num")
                        ),
                        "num", Expression.terminal("[0-9]+")
                )
        );

        @ParameterizedTest
        @CsvSource(
                value = {
                        "double, 1 -1,true",
                        "Triple,1 - 1 -1,true"
                }
        )
        void test(String message, String expr, boolean expected) {
            boolean actual = grammar.fitsGrammar(expr);
            assertEquals(expected, actual, message);
        }
    }
}