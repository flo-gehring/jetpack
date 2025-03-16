package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.grammar.Expression;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.function.Function;

public class GrammarTestUtil {

    public static Function<Symbol.NonTerminal, Expression> emptyGrammar() {
        return _ -> {throw new RuntimeException();};
    }
}
