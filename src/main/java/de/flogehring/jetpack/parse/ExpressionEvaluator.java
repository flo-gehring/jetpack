package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.ConsumedExpression;
import de.flogehring.jetpack.grammar.Expression;
import de.flogehring.jetpack.grammar.Input;

public interface ExpressionEvaluator {

    Either<ConsumedExpression, String> resolveExpression(
            Expression expression,
            Input input,
            int currentPositio
    );
}
