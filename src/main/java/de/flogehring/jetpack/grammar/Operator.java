package de.flogehring.jetpack.grammar;

public sealed interface Operator {

    ConsumedExpression consume(String s, int currentPosition);

    record Sequence(Expression first, Expression second) implements Operator {

        @Override
        public ConsumedExpression consume(String s, int currentPosition) {
            throw new RuntimeException("Not implemented");
        }
    }

    record OrderedChoice(Expression either, Expression or) implements Operator {
        @Override
        public ConsumedExpression consume(String s, int currentPosition) {
            throw new RuntimeException("Not implemented");
        }
    }

    record Star(Expression exp) implements Operator {
        @Override
        public ConsumedExpression consume(String s, int currentPosition) {
            throw new RuntimeException("Not implemented");
        }

    }

    record Optional(Expression exp) implements Operator {
        @Override
        public ConsumedExpression consume(String s, int currentPosition) {
            throw new RuntimeException("Not implemented");
        }

    }

    record Plus(Expression exp) implements Operator {
        @Override
        public ConsumedExpression consume(String s, int currentPosition) {
            throw new RuntimeException("Not implemented");
        }

    }

    record Group(Expression exp) implements Operator {
        @Override
        public ConsumedExpression consume(String s, int currentPosition) {
            throw new RuntimeException("Not implemented");
        }
    }
}
