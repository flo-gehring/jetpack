package de.flogehring.jetpack.grammar;

import java.util.List;

public sealed interface Expression {

    static Expression nonTerminal(String symbol) {
        return new Atomic(new Symbol.NonTerminal(symbol));
    }

    static Expression sequence(Expression first, Expression second) {
        return new Composite(
                new Operator.Sequence(
                        first, second
                )
        );
    }

    static Expression star(Expression exp) {
        return new Composite(
                new Operator.Star(exp)
        );
    }

    static Expression group(Expression exp) {
        return new Composite(
                new Operator.Group(exp)
        );
    }

    static Expression terminal(String terminal) {
        return new Atomic(new Symbol.Terminal(terminal));
    }

    static Expression orderedChoice(Expression firstChoice, Expression secondChoice) {
        return new Expression.Composite(new Operator.OrderedChoice(
                firstChoice,
                secondChoice
        ));
    }

    static Expression optional(Expression exp) {
        return new Expression.Composite(new Operator.Optional(exp));
    }

    static Expression plus(Expression exp) {
        return new Expression.Composite(new Operator.Plus(exp));
    }

    ConsumedExpression consume(String s, int currentPosition);

    record Atomic(Symbol symbols) implements Expression {
        @Override
        public ConsumedExpression consume(String s, int currentPosition) {
            return null;
        }
    }

    record Composite(Operator operator) implements Expression {
        @Override
        public ConsumedExpression consume(String s, int currentPosition) {
            return null;
        }
    }
}
