package de.friendlyhedgehog.jetpack.parse;

import de.friendlyhedgehog.jetpack.datatypes.Either;
import de.friendlyhedgehog.jetpack.grammar.Expression;

interface ExpressionEvaluator {

    Either<ConsumedExpression, String> resolveExpression(
            Expression expression,
            Input input,
            int currentPosition
    );
}
