package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.parse.ConsumedExpression;
import de.flogehring.jetpack.parse.Evaluate;
import de.flogehring.jetpack.parse.Input;
import de.flogehring.jetpack.util.Check;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;

import static de.flogehring.jetpack.grammar.Expression.*;

public class Grammar {

    private final String startingRule;
    private final Map<String, Expression> rules;

    /**
     * See  <a href="https://bford.info/pub/lang/peg.pdf">Paper</a> for details
     */
    private static final Map<String, Expression> grammarGrammar = Map.of(
            "Grammar", plus(nonTerminal("Definition")),
            "Definition", sequence(nonTerminal("Identifier"), terminal("<-"), nonTerminal("Expression")),
            "Expression", sequence(nonTerminal("Sequence"), plus(group(sequence(terminal("/"), nonTerminal("Sequence"))))),
            "Sequence", star(nonTerminal("Prefix")),
            "Prefix", sequence(Expression.questionMark(orderedChoice(terminal("&"), terminal("!"))), nonTerminal("Suffix")),
            "Suffix", sequence(nonTerminal("Primary"), questionMark(orderedChoice(terminal("?"), terminal("*"), terminal("+")))), // TODO make these regex safe
            "Primary", orderedChoice(
                    sequence(nonTerminal("Identifier"), Expression.not(terminal("<-"))),
                    sequence(terminal("\\("), nonTerminal("Expression"), terminal("\\)")),
                    nonTerminal("Literal"),
                    nonTerminal("Class"),
                    terminal("\\.")
            ),
            "Literal", terminal("\"[^\"^\\s]+\""),
            "Identifier", terminal("[a-zA-Z_]+")
    );

    public Grammar(String startingRule, Map<String, Expression> rules) {
        Check.require(
                Check.checkNotNull(startingRule, rules),
                "The parameters to Grammar can't be null."
        );
        Check.require(
                Check.hasKey(rules, startingRule),
                MessageFormat.format(
                        "The starting rule has to be present in the rules. Starting rule {0}, Keys present {1}",
                        startingRule,
                        String.join(", ", rules.keySet())
                )
        );
        this.startingRule = startingRule;
        this.rules = rules;
    }

    public Either<Node<Symbol>, String> parse(String s) {
        return parseString(s).map(consumedExpression ->
                consumedExpression.parseTree().getFirst());
    }

    public boolean fitsGrammar(String s) {
        return parseString(s) instanceof Either.This<ConsumedExpression, String>;
    }

    private Either<ConsumedExpression, String> parseString(String s) {
        Input input = Input.of(s, "\\s");
        Either<ConsumedExpression, String> consume = Evaluate.evaluate(
                input,
                startingRule,
                nonTerminal -> Objects.requireNonNull(rules.get(nonTerminal.name()))
        );

        if (consume instanceof Either.This<ConsumedExpression, String>(var consumedExpression)) {
            if (consumedExpression.parsePosition() == input.length()) {
                return consume;
            }
            return Either.or(
                    "Could only match " + consumedExpression.parsePosition() +
                            " of " + input.length() + " characters"
            );
        } else {
            return consume;
        }
    }
}