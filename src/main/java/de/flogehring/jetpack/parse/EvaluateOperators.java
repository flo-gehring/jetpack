package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.*;

import java.util.function.Function;

import static de.flogehring.jetpack.parse.Evaluate.applyRule;

public class EvaluateOperators {


    public static Either<ConsumedExpression, String> applyOperator(Operator op, Input input, int currentPosition, Function<Symbol.NonTerminal, Expression> grammar, MemoTable memoTable) {
        return switch (op) {
            case Operator.OrderedChoice(var either, var or) -> consumeOrdereChoice(
                    either, or, input, currentPosition, grammar, memoTable
            );
            case Operator.Sequence(var first, var second) -> consumeSequence(
                    first,
                    second,
                    input,
                    currentPosition,
                    grammar,
                    memoTable
            );
            case Operator.Star(var expression) -> consumeStar(
                    expression,
                    input,
                    currentPosition,
                    grammar,
                    memoTable
            );
            case Operator.Plus(var expression) -> consumePlus(
                    expression,
                    input,
                    currentPosition,
                    grammar,
                    memoTable
            );
            case Operator.Optional(var expression) -> consumeOptional(
                    expression,
                    input,
                    currentPosition,
                    grammar,
                    memoTable
            );
            case Operator.Group(var expression) -> applyRule(expression, input, currentPosition, grammar, memoTable);
        };
    }

    public static Either<ConsumedExpression, String> consumeOptional(
            Expression exp,
            Input input,
            int position,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        if (applyRule(exp, input, position, grammar, memoTable) instanceof Either.This<ConsumedExpression, String> success) {
            return success;
        } else {
            return Either.ofThis(new ConsumedExpression(position));
        }

    }

    public static Either<ConsumedExpression, String> consumePlus(
            Expression exp,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        int position = currentPosition;
        Either<ConsumedExpression, String> firstEval = applyRule(exp, input, position, grammar, memoTable);
        return switch (firstEval) {
            case Either.This<ConsumedExpression, String>(var consumedExpression) -> {
                position = consumedExpression.parsePosition();
                Either<ConsumedExpression, String> lastEvaluation;
                do {
                    lastEvaluation = applyRule(exp, input, position, grammar, memoTable);
                    if (lastEvaluation instanceof Either.This<ConsumedExpression, String>(var success)) {
                        position = success.parsePosition();
                    }
                } while (lastEvaluation instanceof Either.This<ConsumedExpression, String>);
                yield Either.ofThis(new ConsumedExpression(position));
            }
            case Either.Or<ConsumedExpression, String> failure -> failure;
        };

    }

    public static Either<ConsumedExpression, String> consumeStar(
            Expression exp,
            Input input,
            int position,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        Either<ConsumedExpression, String> lastEvaluation;
        int lastPosition = position;
        do {
            lastEvaluation = applyRule(exp, input, lastPosition, grammar, memoTable);
            if (lastEvaluation instanceof Either.This<ConsumedExpression, String>(var success)) {
                lastPosition = success.parsePosition();
            }
        } while (lastEvaluation instanceof Either.This<ConsumedExpression, String>);
        return Either.ofThis(new ConsumedExpression(lastPosition));
    }

    public static Either<ConsumedExpression, String> consumeSequence(
            Expression first,
            Expression second,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        Either<ConsumedExpression, String> firstConsume = applyRule(first, input, currentPosition, grammar, memoTable);
        if (firstConsume instanceof Either.This<ConsumedExpression, String>(ConsumedExpression consumed)) {
            return applyRule(second, input, consumed.parsePosition(), grammar, memoTable);
        }
        return firstConsume;
    }

    public static Either<ConsumedExpression, String> consumeOrdereChoice(
            Expression either,
            Expression or,
            Input input,
            int currentPosition,
            Function<Symbol.NonTerminal, Expression> grammar,
            MemoTable memoTable
    ) {
        Either<ConsumedExpression, String> consumeEither = applyRule(either, input, currentPosition, grammar, memoTable);
        return switch (consumeEither) {
            case Either.This<ConsumedExpression, String> ignored -> consumeEither;
            case Either.Or<ConsumedExpression, String> ignored ->
                    applyRule(or, input, currentPosition, grammar, memoTable);
        };
    }
}
