package de.flogehring.jetpack.grammar;

import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public sealed interface Expression permits Symbol, Operator {

    static Expression nonTerminal(String symbol) {
        return new Symbol.NonTerminal(symbol);
    }

    static Expression sequence(Expression first, Expression second) {
        return new Operator.Sequence(first, second);
    }

    static Expression sequence(Expression first, Expression second, Expression... expressions) {
        return applyOperatorInOrder(Operator.Sequence::new, first, second, expressions);
    }

    private static Expression applyOperatorInOrder(BinaryOperator<Expression> combiner, Expression first, Expression second, Expression[] expressions) {
        List<Expression> all = Stream.concat(
                Stream.of(first, second),
                Arrays.stream(expressions)
        ).toList();
        List<Expression> reversed = all.reversed();
        Expression last = reversed.getFirst();
        Expression secondLast = reversed.get(1);
        Expression sequence = combiner.apply(secondLast, last);
        for (int i = 2; i < reversed.size(); ++i) {
            sequence = combiner.apply(reversed.get(i), sequence);
        }
        return sequence;
    }

    static Expression star(Expression exp) {
        return new Operator.Star(exp);
    }

    static Expression group(Expression exp) {
        return new Operator.Group(exp);
    }

    static Expression terminal(String terminal) {
        return new Symbol.Terminal(terminal);
    }

    static Expression orderedChoice(Expression firstChoice, Expression secondChoice) {
        return new Operator.OrderedChoice(
                firstChoice,
                secondChoice
        );
    }

    static Expression orderedChoice(Expression firstChoice, Expression secondChoice, Expression ... nextChoices) {
        return applyOperatorInOrder(Operator.OrderedChoice::new, firstChoice, secondChoice, nextChoices);
    }

    static Expression optional(Expression exp) {
        return new Operator.Optional(exp);
    }

    static Expression plus(Expression exp) {
        return new Operator.Plus(exp);
    }

    static Expression questionMark(Expression expression) {
        throw new RuntimeException();
    }

    static Expression not(Expression terminal) {
        throw new RuntimeException();
    }
}
