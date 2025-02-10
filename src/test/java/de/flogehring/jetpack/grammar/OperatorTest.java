package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.parse.EvaluateOperators;
import de.flogehring.jetpack.parse.MemoTable;
import org.junit.jupiter.api.*;

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
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                            new Operator.Star(Expression.terminal("a")),
                            getInput("ba"),
                            0,
                            GrammarTestUtil.emptyGrammar(),
                            memoTable
                    )
                    .getEither();
            assertEquals(0, consume.parsePosition());
        }

        @Test
        void testMatchMultiple() {
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                            new Operator.Star(Expression.terminal("a")),
                            getInput("baaaab"),
                            1,
                            GrammarTestUtil.emptyGrammar(),
                            memoTable
                    )
                    .getEither();
            assertEquals(5, consume.parsePosition());
        }

        @Disabled
        @Test
        @Timeout(1)
        void testStarInStar() {
            EvaluateOperators.applyOperator(new Operator.Star(Expression.star(Expression.terminal("a"))),
                            getInput("baaaab"),
                            1,
                            GrammarTestUtil.emptyGrammar(),
                            memoTable
                    )
                    .getEither();
        }
    }

    @Nested
    class OrderedChoiceTest {

        @Test
        void testOkFirst() {
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                    new Operator.OrderedChoice(
                            Expression.terminal("a"),
                            Expression.terminal("b")
                    ),
                    getInput("a"),
                    0,
                    GrammarTestUtil.emptyGrammar(),
                    memoTable
            ).getEither();
            assertEquals(consume.parsePosition(), 1);
        }


        @Test
        void testOkSecond() {
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                    new Operator.OrderedChoice(Expression.terminal("a"), Expression.terminal("b")),
                    getInput("b"),
                    0,
                    GrammarTestUtil.emptyGrammar(),
                    memoTable
            ).getEither();
            assertEquals(consume.parsePosition(), 1);
        }

        @Test
        void testFail() {
            var consume = EvaluateOperators.applyOperator(
                    new Operator.OrderedChoice(Expression.terminal("a"), Expression.terminal("b")),
                    getInput("c"),
                    0,
                    GrammarTestUtil.emptyGrammar(),
                    memoTable
            );
            assertInstanceOf(Either.Or.class, consume);
        }
    }

    private static Input getInput(String s) {
        return Input.of(s, "\\s");
    }
}
