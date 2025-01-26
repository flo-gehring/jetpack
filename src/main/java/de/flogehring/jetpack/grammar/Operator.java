package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;

import java.util.function.Function;

public sealed interface Operator extends Expression {

    Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar);

    record Sequence(Expression first, Expression second) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            throw new RuntimeException("Not implemented");
        }
    }

    record OrderedChoice(Expression either, Expression or) implements Operator {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            throw new RuntimeException("Not implemented");
        }
    }

    record Star(Expression exp) implements Operator {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            throw new RuntimeException("Not implemented");
        }

    }

    record Optional(Expression exp) implements Operator {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            throw new RuntimeException("Not implemented");
        }

    }

    record Plus(Expression exp) implements Operator {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            throw new RuntimeException("Not implemented");
        }

    }

    record Group(Expression exp) implements Operator {
        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            throw new RuntimeException("Not implemented");
        }
    }
}
