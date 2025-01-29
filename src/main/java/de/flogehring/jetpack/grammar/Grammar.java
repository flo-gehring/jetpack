package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.util.Check;

import java.util.Map;
import java.util.Objects;

public class Grammar {

    private final String startingRules;
    private final Map<String, Expression> rules;

    public Grammar(String startingRules, Map<String, Expression> rules) {
        Check.require(Check.checkNotNull(startingRules, rules), "Ein Parameter ist null");
        Check.require(Check.hasKey(rules, startingRules), "the starting rule has to be present in the rule map");
        this.startingRules = startingRules;
        this.rules = rules;
    }

    public boolean fitsGrammar(String s) {
        Expression expression = rules.get(startingRules);
        String withoutWhiteSpace = removeAllWhiteSpace(s);
        Either<ConsumedExpression, RuntimeException> consume = expression
                .consume(withoutWhiteSpace,
                        0, nonTerminal -> Objects.requireNonNull(
                                rules.get(nonTerminal.name())
                        ));
        if (consume instanceof Either.This<ConsumedExpression, RuntimeException>(var consumedExpression)) {
            return consumedExpression.parsePosition() == withoutWhiteSpace.length();
        } else {
            return false;
        }
    }

    private static String removeAllWhiteSpace(String s) {
        return s.replaceAll("\\s", "");
    }
}
