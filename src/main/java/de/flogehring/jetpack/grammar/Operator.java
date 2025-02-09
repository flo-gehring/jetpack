package de.flogehring.jetpack.grammar;

public sealed interface Operator extends Expression {

    record Sequence(Expression first, Expression second) implements Operator {

    }

    record OrderedChoice(Expression either, Expression or) implements Operator {

    }

    record Star(Expression exp) implements Operator {

    }

    record Optional(Expression exp) implements Operator {

    }

    record Plus(Expression exp) implements Operator {

    }

    record Group(Expression exp) implements Operator {

    }
}
