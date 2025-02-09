package de.flogehring.jetpack.grammar;


import de.flogehring.jetpack.parse.Grammar;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
    class LeftRecursionTests {

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
        void testHaltsSingle() {
            boolean b = grammar.fitsGrammar(
                    "1"
            );
            assertTrue(b);
        }

        @Test
        void testHalts() {
            boolean b = grammar.fitsGrammar(
                    "1-1"
            );
            assertTrue(b);
        }

        @Test
        @Timeout(value = 1)
        void testHaltsSpace() {
            boolean b = grammar.fitsGrammar(
                    "1 - 1"
            );
            assertTrue(b);
        }

        @Test
        void testHaltsThree() {
            boolean b = grammar.fitsGrammar(
                    "1 - 1 - 1"
            );
            assertTrue(b);
        }

        @Test
        void testHaltsThreeDifferent() {
            boolean b = grammar.fitsGrammar(
                    "1 - 2 - 3"
            );
            assertTrue(b);
        }
    }
}
