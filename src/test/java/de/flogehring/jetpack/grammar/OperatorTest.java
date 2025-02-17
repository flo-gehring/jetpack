package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.parse.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class OperatorTest {

    @Nested
    class StarTest {

        @Test
        void testMatchNone() {
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                            new Operator.Star(Expression.terminal("a")),
                            getInput("ba"),
                            0,
                            createTerminalEvaluator()
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
                            createTerminalEvaluator()
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
                            createTerminalEvaluator()
                    )
                    .getEither();
        }
    }

    private ExpressionEvaluator createTerminalEvaluator() {
        return (expression, input, currentPositio) -> switch (expression) {
            case Operator op -> EvaluateOperators.applyOperator(
                    op,
                    input,
                    currentPositio,
                    createTerminalEvaluator()
            );
            case Symbol symbol -> extracted(input, currentPositio, symbol);
        };
    }

    private Either<ConsumedExpression, String> extracted(Input input, int currentPositio, Symbol symbol) {
        return switch (symbol) {
            case Symbol.Terminal t -> EvaluateTerminal.applyTerminal(t.symbol(), input, currentPositio);
            case null, default -> throw new RuntimeException();
        };
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
                    createTerminalEvaluator()
            ).getEither();
            assertEquals(1, consume.parsePosition());
        }


        @Test
        void testOkSecond() {
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                    new Operator.OrderedChoice(Expression.terminal("a"), Expression.terminal("b")),
                    getInput("b"),
                    0,
                    createTerminalEvaluator()
            ).getEither();
            assertEquals(1, consume.parsePosition());
        }

        @Test
        void testFail() {
            var consume = EvaluateOperators.applyOperator(
                    new Operator.OrderedChoice(Expression.terminal("a"), Expression.terminal("b")),
                    getInput("c"),
                    0,
                    createTerminalEvaluator()
            );
            assertInstanceOf(Either.Or.class, consume);
        }
    }

    private static Input getInput(String s) {
        return Input.of(s, "\\s");
    }
}
