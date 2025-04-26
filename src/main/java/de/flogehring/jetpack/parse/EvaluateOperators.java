package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Expression;
import de.flogehring.jetpack.grammar.Operator;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class EvaluateOperators {


    private EvaluateOperators() {
    }

    public static Either<ConsumedExpression, String> applyOperator(
            Operator op,
            Input input,
            int currentPosition,
            ExpressionEvaluator evaluator
    ) {
        return switch (op) {
            case Operator.OrderedChoice(var either, var or) -> consumeOrdereChoice(
                    either, or, input, currentPosition, evaluator
            );
            case Operator.Sequence(var first, var second) -> consumeSequence(
                    first,
                    second,
                    input,
                    currentPosition,
                    evaluator
            );
            case Operator.Star(var expression) -> consumeStar(
                    expression,
                    input,
                    currentPosition,
                    evaluator
            );
            case Operator.Plus(var expression) -> consumePlus(
                    expression,
                    input,
                    currentPosition,
                    evaluator
            );
            case Operator.Optional(var expression) -> consumeOptional(
                    expression,
                    input,
                    currentPosition,
                    evaluator
            );
            case Operator.Not(var expression) -> consumeNot(
                    expression,
                    input,
                    currentPosition,
                    evaluator
            );
            case Operator.Group(var expression) -> evaluator.resolveExpression(expression, input, currentPosition);
        };
    }

    private static Either<ConsumedExpression, String> consumeNot(Expression expression, Input input, int currentPosition, ExpressionEvaluator evaluator) {
        return switch (evaluator.resolveExpression(expression, input, currentPosition)) {
            case Either.This<ConsumedExpression, String>(var _) -> Either.or("Matched on not predicate");
            case Either.Or<ConsumedExpression, String> _ -> Either.ofThis(
                    new ConsumedExpression(
                            currentPosition,
                            List.of()
                    )
            );
        };
    }

    private static Either<ConsumedExpression, String> consumeOptional(
            Expression exp,
            Input input,
            int position,
            ExpressionEvaluator expressionEvaluator
    ) {
        if (expressionEvaluator.resolveExpression(exp, input, position) instanceof Either.This<ConsumedExpression, String> success) {
            return success;
        } else {
            return Either.ofThis(new ConsumedExpression(
                    position,
                    List.of()
            ));
        }
    }

    private static Either<ConsumedExpression, String> consumePlus(
            Expression exp,
            Input input,
            int currentPosition,
            ExpressionEvaluator evaluator
    ) {
        int position = currentPosition;
        Either<ConsumedExpression, String> firstEval = evaluator.resolveExpression(exp, input, position);

        return switch (firstEval) {
            case Either.This<ConsumedExpression, String>(var consumedExpression) -> {
                List<Node<Symbol>> subresults = new ArrayList<>(consumedExpression.parseTree());
                position = consumedExpression.parsePosition();
                Either<ConsumedExpression, String> lastEvaluation;
                do {
                    lastEvaluation = evaluator.resolveExpression(exp, input, position);
                    if (lastEvaluation instanceof Either.This<ConsumedExpression, String>(var success)) {
                        position = success.parsePosition();
                        subresults.addAll(success.parseTree());
                    }
                } while (lastEvaluation instanceof Either.This<ConsumedExpression, String>);
                yield Either.ofThis(new ConsumedExpression(position, subresults));
            }
            case Either.Or<ConsumedExpression, String> failure -> failure;
        };
    }

    private static Either<ConsumedExpression, String> consumeStar(
            Expression exp,
            Input input,
            int position,
            ExpressionEvaluator evaluator
    ) {
        Either<ConsumedExpression, String> lastEvaluation;
        int lastPosition = position;
        List<Node<Symbol>> subresults = new ArrayList<>();
        do {
            lastEvaluation = evaluator.resolveExpression(exp, input, lastPosition);
            if (lastEvaluation instanceof Either.This<ConsumedExpression, String>(var success)) {
                subresults.addAll(success.parseTree());
                lastPosition = success.parsePosition();
            }
        } while (lastEvaluation instanceof Either.This<ConsumedExpression, String>);
        return Either.ofThis(new ConsumedExpression(lastPosition, subresults));
    }

    private static Either<ConsumedExpression, String> consumeSequence(
            Expression first,
            Expression second,
            Input input,
            int currentPosition,
            ExpressionEvaluator expressionEvaluator
    ) {
        Either<ConsumedExpression, String> firstConsumedExpression = expressionEvaluator
                .resolveExpression(first, input, currentPosition);
        if (firstConsumedExpression instanceof Either.This<ConsumedExpression, String>(
                ConsumedExpression firstSuccessfulParse
        )) {
            Either<ConsumedExpression, String> secondConsumedExpression = expressionEvaluator
                    .resolveExpression(
                            second,
                            input,
                            firstSuccessfulParse.parsePosition());
            return secondConsumedExpression.map(
                    secondSuccessfulParse ->
                            new ConsumedExpression(
                                    secondSuccessfulParse.parsePosition(),
                                    Stream.concat(firstSuccessfulParse.parseTree().stream(), secondSuccessfulParse.parseTree().stream()).toList()
                            )
            );
        }
        return firstConsumedExpression;
    }

    public static Either<ConsumedExpression, String> consumeOrdereChoice(
            Expression either,
            Expression or,
            Input input,
            int currentPosition,
            ExpressionEvaluator evaluator
    ) {
        Either<ConsumedExpression, String> consumeEither = evaluator.resolveExpression(either, input, currentPosition);
        return switch (consumeEither) {
            case Either.This<ConsumedExpression, String> ignored -> consumeEither;
            case Either.Or<ConsumedExpression, String> ignored ->
                    evaluator.resolveExpression(or, input, currentPosition);
        };
    }
}
