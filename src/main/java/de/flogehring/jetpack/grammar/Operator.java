package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;

import java.util.function.Function;

public sealed interface Operator extends Expression {

    Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar);

    record Sequence(Expression first, Expression second) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            Either<ConsumedExpression, RuntimeException> firstConsume = first.consume(s, currentPosition, grammar);
            if (firstConsume instanceof Either.This<ConsumedExpression, RuntimeException>(var consumed)) {
                return second.consume(s, consumed.parsePosition(), grammar);
            }
            return firstConsume;
        }
    }

    record OrderedChoice(Expression either, Expression or) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(
                String s,
                int currentPosition,
                Function<Symbol.NonTerminal, Expression> grammar
        ) {
            Either<ConsumedExpression, RuntimeException> consumeEither = either.consume(s, currentPosition, grammar);
            return switch (consumeEither) {
                case Either.This<ConsumedExpression, RuntimeException> ignored -> consumeEither;
                case Either.Or<ConsumedExpression, RuntimeException> ignored -> or.consume(s, currentPosition, grammar);
            };
        }
    }

    record Star(Expression exp) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(
                String s,
                int currentPosition,
                Function<Symbol.NonTerminal, Expression> grammar
        ) {
            int position = currentPosition;
            Either<ConsumedExpression, RuntimeException> lastEvaluation;
            do {
                lastEvaluation = exp.consume(s, position, grammar);
                if (lastEvaluation instanceof Either.This<ConsumedExpression, RuntimeException>(var success)) {
                    position = success.parsePosition();
                }
            } while (lastEvaluation instanceof Either.This<ConsumedExpression, RuntimeException>);
            return Either.ofThis(new ConsumedExpression(position));
        }
    }

    record Optional(Expression exp) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            if (exp.consume(s, currentPosition, grammar) instanceof Either.This<ConsumedExpression, RuntimeException> success) {
                return success;
            } else {
                return Either.ofThis(new ConsumedExpression(currentPosition));
            }
        }
    }

    record Plus(Expression exp) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            int position = currentPosition;
            Either<ConsumedExpression, RuntimeException> firstEval = exp.consume(s, position, grammar);
            return switch (firstEval) {
                case Either.This<ConsumedExpression, RuntimeException> firstEvaluation -> {
                    Either<ConsumedExpression, RuntimeException> lastEvaluation = firstEvaluation;
                    do {
                        lastEvaluation = exp.consume(s, position, grammar);
                        if (lastEvaluation instanceof Either.This<ConsumedExpression, RuntimeException>(var success)) {
                            position = success.parsePosition();
                        }
                    } while (lastEvaluation instanceof Either.This<ConsumedExpression, RuntimeException>);
                    yield Either.ofThis(new ConsumedExpression(position));
                }
                case Either.Or<ConsumedExpression, RuntimeException> failure -> failure;
            };
        }
    }

    record Group(Expression exp) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(String s, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar) {
            return exp.consume(s, currentPosition, grammar);
        }
    }
}
