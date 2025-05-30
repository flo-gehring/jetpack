package de.friendlyhedgehog.jetpack.grammar;

import de.friendlyhedgehog.jetpack.util.Check;

import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public sealed interface Expression permits Symbol, Operator {

    static Expression nonTerminal(String symbol) {
        return new Symbol.NonTerminal(symbol);
    }

    static Operator.Sequence sequence(Expression first, Expression second) {
        return new Operator.Sequence(first, second);
    }

    static Expression sequence(List<Expression> expressions) {
        // TODO create empty expression and return
        Check.require(!expressions.isEmpty(), "Can't construct sequence from empty list");
        if (expressions.isEmpty()) {
            return Symbol.empty();
        }
        if (expressions.size() == 1) {
            return expressions.getFirst();
        }
        return sequence(expressions.getFirst(),
                expressions.get(1), expressions.subList(2, expressions.size()).toArray(new Expression[]{})
        );
    }

    static Expression orderedChoice(List<Expression> expressions) {
        if (expressions.isEmpty()) {
            return Expression.empty();
        }
        if (expressions.size() == 1) {
            return expressions.getFirst();
        }
        return orderedChoice(
                expressions.getFirst(),
                expressions.get(1),
                expressions.subList(2, expressions.size()).toArray(new Expression[]{})
        );
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

    static Expression terminalLiteral(String terminal) {
        Check.requireNotEmpty(terminal);
        return new Symbol.Terminal(java.util.regex.Pattern.quote(terminal));
    }

    static Expression orderedChoice(Expression firstChoice, Expression secondChoice) {
        return new Operator.OrderedChoice(
                firstChoice,
                secondChoice
        );
    }

    static Expression orderedChoice(Expression firstChoice, Expression secondChoice, Expression... nextChoices) {
        return applyOperatorInOrder(Operator.OrderedChoice::new, firstChoice, secondChoice, nextChoices);
    }

    static Expression optional(Expression expression) {
        return new Operator.Optional(expression);
    }

    static Expression plus(Expression expression) {
        return new Operator.Plus(expression);
    }

    static Expression not(Expression expression) {
        return new Operator.Not(expression);
    }

    static Expression and(Expression expression) {
        return new Operator.And(expression);
    }

    static Expression empty() {
        return Symbol.empty();
    }
}