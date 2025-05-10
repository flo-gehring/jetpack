package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.Expression;
import de.flogehring.jetpack.grammar.Operator;
import de.flogehring.jetpack.grammar.Symbol;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static de.flogehring.jetpack.grammar.Expression.*;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.assertj.core.api.Assertions.*;
public class OperatorTest {

    @Nested
    class StarTest {

        @Test
        void testMatchNone() {
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                            new Operator.Star(terminal("a")),
                            getInput("ba"),
                            0,
                            createExpressionEvaluator()
                    )
                    .getEither();
            assertEquals(0, consume.parsePosition());
        }

        @Test
        void testMatchMultiple() {
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                            new Operator.Star(terminal("a")),
                            getInput("baaaab"),
                            1,
                            createExpressionEvaluator()
                    )
                    .getEither();
            assertEquals(5, consume.parsePosition());
        }

        @Test
        @Timeout(1)
        void testStarInStar() {
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> Expression.star(Expression.star(terminal("a"))));
        }
    }

    private ExpressionEvaluator createExpressionEvaluator() {
        return (expression, input, currentPosition) -> switch (expression) {
            case Operator op -> EvaluateOperators.applyOperator(
                    op,
                    input,
                    currentPosition,
                    createExpressionEvaluator()
            );
            case Symbol symbol -> terminalEvaluator(input, currentPosition, symbol);
        };
    }

    private Either<ConsumedExpression, String> terminalEvaluator(Input input, int currentPositio, Symbol symbol) {
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
                            terminal("a"),
                            terminal("b")
                    ),
                    getInput("a"),
                    0,
                    createExpressionEvaluator()
            ).getEither();
            assertEquals(1, consume.parsePosition());
        }


        @Test
        void testOkSecond() {
            ConsumedExpression consume = EvaluateOperators.applyOperator(
                    new Operator.OrderedChoice(terminal("a"), terminal("b")),
                    getInput("b"),
                    0,
                    createExpressionEvaluator()
            ).getEither();
            assertEquals(1, consume.parsePosition());
        }

        @Test
        void testFail() {
            var consume = EvaluateOperators.applyOperator(
                    new Operator.OrderedChoice(terminal("a"), terminal("b")),
                    getInput("c"),
                    0,
                    createExpressionEvaluator()
            );
            assertInstanceOf(Either.Or.class, consume);
        }
    }

    @Nested
    class AndTest {

        @Test
        void conditionSatisfied () {
            Either<ConsumedExpression, String> consume = EvaluateOperators.applyOperator(
                    sequence(
                            and(sequence(plus(terminal("\\w")), terminalLiteral("!"))),
                            plus(terminal(".+"))
                    ),
                    getInput("this should match!"),
                    0,
                    createExpressionEvaluator()
            );
            assertInstanceOf( Either.This.class, consume);
            ConsumedExpression either = consume.getEither();
            assertThat(either.parseTree()).hasSize(3);
        }

        @Test
        void conditionViolated () {
            Either<ConsumedExpression, String> consume = EvaluateOperators.applyOperator(
                    sequence(
                            and(sequence(plus(terminal("\\w")), terminalLiteral("!"))),
                            plus(terminal(".+"))
                    ),
                    getInput("this should not match"),
                    0,
                    createExpressionEvaluator()
            );
            assertInstanceOf( Either.Or.class, consume);
        }
    }

    private static Input getInput(String s) {
        return Input.of(s, "\\s");
    }
}
