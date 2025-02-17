package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.parse.ConsumedExpression;
import de.flogehring.jetpack.parse.EvaluateTerminal;
import de.flogehring.jetpack.parse.Input;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SymbolTest {

    @Nested
    class TerminalTest {

        @ParameterizedTest
        @CsvSource(value = {
                "Simple Testcase,a,abcd,0,1",
                "Multiple Possible matches,a,aaaa,2,3",
                "Longer Terminal,static,public static void main,6,12",
                "Group of numbers,[0-9]+,1234Hello,0,4",
                "Greedy Star,a*,bcdf,0,0"
        })
        void testOk(
                String testMessage,
                String symbol,
                String inputString,
                int initialOffset,
                int expectedOffsetConsumed
        ) {
            Either<ConsumedExpression, String> consume = EvaluateTerminal.applyTerminal(
                    symbol, Input.of(
                            inputString,
                            "\\s"
                    ), initialOffset
            );

            if (consume instanceof Either.This<ConsumedExpression, String> actual) {
                assertEquals(expectedOffsetConsumed, actual.get().parsePosition(), testMessage);
            } else {
                fail("Failed to parse: " + testMessage, new RuntimeException(consume.getOr()));
            }
        }
    }
}
