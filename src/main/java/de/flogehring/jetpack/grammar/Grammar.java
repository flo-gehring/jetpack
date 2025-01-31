package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.util.Check;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

public class Grammar {

    private final String startingRules;
    private final Map<String, Expression> rules;

    public Grammar(String startingRules, Map<String, Expression> rules) {
        Check.require(
                Check.checkNotNull(startingRules, rules),
                "The parameters to Grammar can't be null."
        );
        Check.require(
                Check.hasKey(rules, startingRules),
                MessageFormat.format(
                        "The starting rule has to be present in the rules. Starting rule {0}, Keys present {1}",
                        startingRules,
                        String.join(", ", rules.keySet())
                )
        );
        this.startingRules = startingRules;
        this.rules = rules;
    }

    public boolean fitsGrammar(String s) {
        Expression expression = rules.get(startingRules);
        Input input = Input.of(s, "\\s");
        Either<ConsumedExpression, RuntimeException> consume = expression
                .consume(input,
                        0, nonTerminal -> Objects.requireNonNull(
                                rules.get(nonTerminal.name())
                        ));
        if (consume instanceof Either.This<ConsumedExpression, RuntimeException>(var consumedExpression)) {
            return consumedExpression.parsePosition() == input.length();
        } else {
            return false;
        }
    }
}
