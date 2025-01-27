package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class OperatorTest {


    @Nested
    class StarTest {

        @Test
        void testMatchNone() {
            Expression expression = Expression.star(Expression.terminal("a"));
            ConsumedExpression consume = expression.consume("ba", 0, GrammarTestUtil.emptyGrammar())
                    .getEither();
            assertEquals(0, consume.parsePosition());
        }

        @Test
        void testMatchMultiple() {
            Expression expression = Expression.star(Expression.terminal("a"));
            ConsumedExpression consume = expression.consume("baaaab", 1, GrammarTestUtil.emptyGrammar())
                    .getEither();
            assertEquals(5, consume.parsePosition());
        }
    }

    @Nested
    class OrderedChoiceTest {

        @Test
        void testOkFirst() {
            Expression expression = Expression.orderedChoice(Expression.terminal("a"), Expression.terminal("b"));
            ConsumedExpression consume = expression.consume("a", 0, GrammarTestUtil.emptyGrammar()).getEither();
            assertEquals(consume.parsePosition(), 1);
        }


        @Test
        void testOkSecond() {
            Expression expression = Expression.orderedChoice(Expression.terminal("a"), Expression.terminal("b"));
            ConsumedExpression consume = expression.consume("b", 0, GrammarTestUtil.emptyGrammar()).getEither();
            assertEquals(consume.parsePosition(), 1);
        }

        @Test
        void testFail() {
            Expression expression = Expression.orderedChoice(Expression.terminal("a"), Expression.terminal("b"));
            var consume = expression.consume("c", 0, GrammarTestUtil.emptyGrammar());
            assertInstanceOf(Either.Or.class, consume);
        }
    }
}
