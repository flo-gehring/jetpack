package de.flogehring.jetpack.grammar;

import java.util.function.Function;

public class GrammarTestUtil {

    public static Function<Symbol.NonTerminal, Expression> emptyGrammar() {
        return _ -> {throw new RuntimeException();};
    }
}
