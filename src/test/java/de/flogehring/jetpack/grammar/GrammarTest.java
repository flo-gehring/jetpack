package de.flogehring.jetpack.grammar;


import de.flogehring.jetpack.parse.Grammar;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrammarTest {

    @Nested
    class MathGrammarTests {

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
                                                Expression.orderedChoice(
                                                        Expression.terminal("\\+"),
                                                        Expression.terminal("-")
                                                ),
                                                Expression.nonTerminal("Product")
                                        )
                                ))),
                        "Product", Expression.sequence(
                                Expression.nonTerminal("Power"),
                                Expression.star(Expression.group(
                                        Expression.sequence(
                                                Expression.orderedChoice(
                                                        Expression.terminal("\\*"),
                                                        Expression.terminal("/")
                                                ),
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
                                Expression.plus(Expression.terminal("[0-9]")),
                                Expression.sequence(
                                        Expression.sequence(
                                                Expression.terminal("\\("),
                                                Expression.nonTerminal("Expr")
                                        ),
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

        @CsvSource(value = {
                "Single Number,1,true",
                "Two Numbers,1-1,true",
                "Three Numbers,1-1-1,true",
                "Multiple Different,1 - 1 -2 -3-4-1,true",
                "Does not match because expr not completed,1 - ,false",
                "Completely wrong,1 - asdf ,false"
        })
        @ParameterizedTest
        void test(String testMessage, String expression, boolean expected) {
            boolean actual = grammar.fitsGrammar(
                    expression
            );
            assertEquals(expected, actual, testMessage);
        }
    }

    @Nested
    class IndirectLeftRecursionTwo {

        Grammar g = new Grammar(
                "Expr",
                Map.of(
                        "Expr", Expression.orderedChoice(
                                Expression.sequence(Expression.nonTerminal("Expr"), Expression.sequence(Expression.terminal("\\+"), Expression.nonTerminal("Num"))),
                                Expression.nonTerminal("Num")

                        ),
                        "Num", Expression.orderedChoice(
                                Expression.sequence(Expression.nonTerminal("Num"), Expression.nonTerminal("Digit")),
                                Expression.nonTerminal("Digit")
                        ),
                        "Digit", Expression.terminal("[0-9]")

                )
        );

        @Test
        void parse() {
            assertTrue(
                    g.fitsGrammar("12 + 3")
            );
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