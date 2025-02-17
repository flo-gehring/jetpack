package de.flogehring.jetpack.grammar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExpressionTest {

    @Test
    void sequence() {
        Expression sequence = Expression.sequence(
                Expression.terminal("hello"),
                Expression.terminal("world"),
                Expression.terminal("here"),
                Expression.terminal("i"),
                Expression.terminal("come")
        );
        Expression expected = new Operator.Sequence(
                Expression.terminal("hello"),
                new Operator.Sequence(Expression.terminal("world"),
                        new Operator.Sequence(Expression.terminal("here"),
                                new Operator.Sequence(Expression.terminal("i"),
                                        Expression.terminal("come")
                                ))));
        assertEquals(expected, sequence);
    }
}
