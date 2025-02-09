package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.parse.MemoTable;

import java.util.function.Function;

public sealed interface Operator extends Expression {

    Either<ConsumedExpression, RuntimeException> consume(Input input, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar, MemoTable memoTable);

    record Sequence(Expression first, Expression second) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(
                Input input,
                int currentPosition,
                Function<Symbol.NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            Either<ConsumedExpression, RuntimeException> firstConsume = first.consume(input, currentPosition, grammar, memoTable);
            if (firstConsume instanceof Either.This<ConsumedExpression, RuntimeException>(var consumed)) {
                return second.consume(input, consumed.parsePosition(), grammar, memoTable);
            }
            return firstConsume;
        }
    }

    record OrderedChoice(Expression either, Expression or) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(
                Input input,
                int currentPosition,
                Function<Symbol.NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            Either<ConsumedExpression, RuntimeException> consumeEither = either.consume(input, currentPosition, grammar, memoTable);
            return switch (consumeEither) {
                case Either.This<ConsumedExpression, RuntimeException> ignored -> consumeEither;
                case Either.Or<ConsumedExpression, RuntimeException> ignored ->
                        or.consume(input, currentPosition, grammar, memoTable);
            };
        }
    }

    record Star(Expression exp) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(
                Input input,
                int currentPosition,
                Function<Symbol.NonTerminal, Expression> grammar,
                MemoTable memoTable) {
            int position = currentPosition;
            Either<ConsumedExpression, RuntimeException> lastEvaluation;
            do {
                lastEvaluation = exp.consume(input, position, grammar, memoTable);
                if (lastEvaluation instanceof Either.This<ConsumedExpression, RuntimeException>(var success)) {
                    position = success.parsePosition();
                }
            } while (lastEvaluation instanceof Either.This<ConsumedExpression, RuntimeException>);
            return Either.ofThis(new ConsumedExpression(position));
        }
    }

    record Optional(Expression exp) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(
                Input input,
                int currentPosition,
                Function<Symbol.NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            if (exp.consume(input, currentPosition, grammar, memoTable) instanceof Either.This<ConsumedExpression, RuntimeException> success) {
                return success;
            } else {
                return Either.ofThis(new ConsumedExpression(currentPosition));
            }
        }
    }

    record Plus(Expression exp) implements Operator {

        @Override
        public Either<ConsumedExpression, RuntimeException> consume(
                Input input,
                int currentPosition,
                Function<Symbol.NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            int position = currentPosition;
            Either<ConsumedExpression, RuntimeException> firstEval = exp.consume(input, position, grammar, memoTable);
            return switch (firstEval) {
                case Either.This<ConsumedExpression, RuntimeException>(var consumedExpression) -> {
                    position = consumedExpression.parsePosition();
                    Either<ConsumedExpression, RuntimeException> lastEvaluation;
                    do {
                        lastEvaluation = exp.consume(input, position, grammar, memoTable);
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
        public Either<ConsumedExpression, RuntimeException> consume(
                Input input,
                int currentPosition,
                Function<Symbol.NonTerminal, Expression> grammar,
                MemoTable memoTable
        ) {
            return exp.consume(input, currentPosition, grammar, memoTable);
        }
    }
}
