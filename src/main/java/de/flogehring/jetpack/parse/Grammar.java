package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.grammar.ConsumedExpression;
import de.flogehring.jetpack.grammar.Expression;
import de.flogehring.jetpack.grammar.Input;
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
        Either<ConsumedExpression, String> consume = Evaluate.applyRule(
                expression,
                input,
                0,
                nonTerminal -> Objects.requireNonNull(
                        rules.get(nonTerminal.name())
                ),
                MemoTable.of()
        );

        if (consume instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            System.out.println("Not Enough tokens matched");
            return consumedExpression.parsePosition() == input.length();
        } else {
            System.out.println("Failed match because " + consume.getOr());
            return false;
        }
    }
}
