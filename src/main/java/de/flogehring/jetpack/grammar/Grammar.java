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
            "Definition", sequence(nonTerminal("Identifier"), terminalLiteral("<-"), nonTerminal("Expression")),
            "Expression", sequence(nonTerminal("Sequence"), star(group(sequence(terminalLiteral("/"), nonTerminal("Sequence"))))),
            "Sequence", star(nonTerminal("Prefix")),
            "Prefix", sequence(Expression.questionMark(orderedChoice(terminalLiteral("&"), terminalLiteral("!"))), nonTerminal("Suffix")),
            "Suffix", sequence(nonTerminal("Primary"),
                    questionMark(orderedChoice(terminalLiteral("?"), terminalLiteral("*"), terminalLiteral("+")))
            ),
            "Primary", orderedChoice(
                    sequence(nonTerminal("Identifier"), Expression.not(terminalLiteral("<-"))),
                    sequence(terminalLiteral("("), nonTerminal("Expression"), terminalLiteral(")")),
                    nonTerminal("Literal"),
                    nonTerminal("Class"),
                    terminalLiteral(".")
            ),
            "Literal", terminal("\"[^\"]+\""),
            "Class", terminal("\"[^\"]+\""),
            "Identifier", terminal("[a-zA-Z_]+")
    );

    public static Either<Grammar, String> of(String grammarDefinition) {
        Grammar parsingGrammar = new Grammar(
                "Grammar",
                grammarGrammar
        );

        Either<ConsumedExpression, String> consumedExpressionStringEither = parsingGrammar.parseString(grammarDefinition);
        return consumedExpressionStringEither.flatMap(Grammar::createGrammar);

    }

    private static Either<Grammar, String> createGrammar(ConsumedExpression consumedExpression) {
        System.out.println(consumedExpression.parseTree().getFirst());
        throw new RuntimeException("Not implemented yet!");
    }

    public Grammar(String startingRule, Map<String, Expression> rules) {
        Check.require(
                Check.requireNotNull(startingRule, rules),
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
                nonTerminal -> Objects.requireNonNull(
                        rules.get(nonTerminal.name()),
                        () -> "Could not resolve Rule with name " + nonTerminal.name()
                )
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