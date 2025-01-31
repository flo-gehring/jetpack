package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.parse.MemoTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class OperatorTest {

    MemoTable memoTable;

    @BeforeEach
    void beforeEach() {
        memoTable = MemoTable.of();
    }

    @Nested
    class StarTest {


        @Test
        void testMatchNone() {
            Expression expression = Expression.star(Expression.terminal("a"));
            ConsumedExpression consume = expression.consume(getInput("ba"), 0, GrammarTestUtil.emptyGrammar(), memoTable)
                    .getEither();
            assertEquals(0, consume.parsePosition());
        }

        @Test
        void testMatchMultiple() {
            Expression expression = Expression.star(Expression.terminal("a"));
            ConsumedExpression consume = expression.consume(getInput("baaaab"), 1, GrammarTestUtil.emptyGrammar(), memoTable)
                    .getEither();
            assertEquals(5, consume.parsePosition());
        }
    }

    @Nested
    class OrderedChoiceTest {

        @Test
        void testOkFirst() {
            Expression expression = Expression.orderedChoice(Expression.terminal("a"), Expression.terminal("b"));
            ConsumedExpression consume = expression.consume(getInput("a"), 0, GrammarTestUtil.emptyGrammar(), memoTable).getEither();
            assertEquals(consume.parsePosition(), 1);
        }


        @Test
        void testOkSecond() {
            Expression expression = Expression.orderedChoice(Expression.terminal("a"), Expression.terminal("b"));
            ConsumedExpression consume = expression.consume(getInput("b"), 0, GrammarTestUtil.emptyGrammar(), memoTable).getEither();
            assertEquals(consume.parsePosition(), 1);
        }

        @Test
        void testFail() {
            Expression expression = Expression.orderedChoice(Expression.terminal("a"), Expression.terminal("b"));
            var consume = expression.consume(getInput("c"), 0, GrammarTestUtil.emptyGrammar(), memoTable);
            assertInstanceOf(Either.Or.class, consume);
        }
    }

    private static Input getInput(String s) {
        return Input.of(s, "\\s");
    }
}
