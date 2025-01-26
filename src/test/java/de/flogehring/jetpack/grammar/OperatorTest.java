package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class OperatorTest {

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
