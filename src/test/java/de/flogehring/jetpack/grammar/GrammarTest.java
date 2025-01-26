package de.flogehring.jetpack.grammar;


import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
                                                        Expression.terminal("+"),
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
                                                        Expression.terminal("*"),
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
                                                Expression.sequence(Expression.terminal("^"), Expression.nonTerminal("Power"))
                                        )
                                )
                        ),
                        "Value", Expression.orderedChoice(
                                Expression.plus(Expression.terminal("[0-9]")),
                                Expression.sequence(
                                        Expression.sequence(
                                                Expression.terminal("("),
                                                Expression.nonTerminal("Expr")
                                        ),
                                        Expression.terminal(")")
                                )
                        )
                )
        );
    }
}
