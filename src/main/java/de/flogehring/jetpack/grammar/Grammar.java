package de.flogehring.jetpack.grammar;

import java.util.Map;

public class Grammar {

    private final String startingRules;
    private final Map<String, Expression> rules;

    public Grammar(String startingRules, Map<String, Expression> rules) {
        this.startingRules = startingRules;
        this.rules = rules;
    }

    public boolean fitsGrammar(String s) {
        throw new RuntimeException("Wurde noch nicht implementiert");
    }
}
