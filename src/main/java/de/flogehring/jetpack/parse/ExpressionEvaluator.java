package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.Expression;

interface ExpressionEvaluator {

    Either<ConsumedExpression, String> resolveExpression(
            Expression expression,
            Input input,
            int currentPosition
    );
}
