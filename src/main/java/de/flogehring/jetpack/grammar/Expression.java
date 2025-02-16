package de.flogehring.jetpack.grammar;

public sealed interface Expression permits Symbol, Operator {

    static Expression nonTerminal(String symbol) {
        return new Symbol.NonTerminal(symbol);
    }

    static Expression sequence(Expression first, Expression second) {
        return new Operator.Sequence(first, second);
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

    static Expression optional(Expression exp) {
        return new Operator.Optional(exp);
    }

    static Expression plus(Expression exp) {
        return new Operator.Plus(exp);
    }
}
