package de.flogehring.jetpack.grammar;

import de.flogehring.jetpack.datatypes.Either;
import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.parse.ConsumedExpression;
import de.flogehring.jetpack.parse.Evaluate;
import de.flogehring.jetpack.parse.Input;
import de.flogehring.jetpack.util.Check;
import lombok.Getter;
import lombok.ToString;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;

import static de.flogehring.jetpack.grammar.Expression.*;

@ToString
@Getter
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
            "Prefix", sequence(optional(orderedChoice(terminalLiteral("&"), terminalLiteral("!"))), nonTerminal("Suffix")),
            "Suffix", sequence(nonTerminal("Primary"),
                    optional(orderedChoice(terminalLiteral("?"), terminalLiteral("*"), terminalLiteral("+")))
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
        Node<Symbol> parseTree = consumedExpression.parseTree().getFirst();
        Symbol grammar = parseTree.getValue();
        Check.require(grammar.equals(nonTerminal("Grammar")),
                "Parse tree does not start with grammar definition");
        Map<String, Expression> rules = new HashMap<>();
        String firstRule = null;
        for (Node<Symbol> definition : parseTree.getChildren()) {
            Check.require(
                    definition.getValue().equals(Symbol.nonTerminal("Definition")),
                    "Wrong nonterminal"
            );

            String ruleName = getTerminalChild(definition.getChildren().getFirst());
            if (firstRule == null) {
                firstRule = ruleName;
            }
            rules.put(
                    ruleName,
                    parseExpression(definition.getChildren().get(2))
            );
        }
        return Either.ofThis(
                new Grammar(
                        firstRule,
                        rules
                )
        );
    }

    private static Expression parseExpression(Node<Symbol> expressionRule) {
        if (expressionRule.getValue() instanceof Symbol.NonTerminal) {
            return parseExpressionRule(expressionRule.getChildren());
        }
        throw new RuntimeException("https://xkcd.com/2200/");
    }

    private static Expression parseExpressionRule(List<Node<Symbol>> children) {
        Node<Symbol> sequence = children.getFirst();
        Expression firstExpression = parseSequence(sequence.getChildren());
        List<Expression> expressions = new ArrayList<>(List.of(firstExpression));
        if (children.size() > 1) {
            for (int i = 2; i < children.size(); i += 2) {
                expressions.add(parseSequence(children.get(i).getChildren()));
            }
        }
        return Expression.orderedChoice(expressions);
    }

    private static Expression parseSequence(List<Node<Symbol>> children) {
        return Expression.sequence(children.stream().map(child -> parsePrefix(child.getChildren())).toList());
    }

    private static Expression parsePrefix(List<Node<Symbol>> children) {
        Node<Symbol> mainExpression;
        Function<Expression, Expression> wrapper;
        if (children.size() > 1) {
            String prefix = getTerminalChild(children.getFirst());
            Check.require(prefix.equals("&") || prefix.equals("!"), "Prefix must be either & or !");
            wrapper = prefix.equals("&") ? Expression::and : Expression::not;
            mainExpression = children.get(1);
        } else {
            mainExpression = children.getFirst();
            wrapper = Function.identity();
        }
        return wrapper.apply(parseSuffix(mainExpression.getChildren()));
    }

    private static Expression parseSuffix(List<Node<Symbol>> children) {
        Function<Expression, Expression> wrapper = Function.identity();
        if (children.size() > 1) {
            String suffix = getTerminal(children.get(1));
            wrapper = switch (suffix) {
                case "*" -> Expression::star;
                case "+" -> Expression::plus;
                case "?" -> Expression::optional;
                default -> throw new RuntimeException("Unrecognized Suffix: " + suffix);
            };
        }
        return wrapper.apply(parsePrimary(children.getFirst().getChildren()));
    }

    private static String getTerminal(Node<Symbol> node) {
        if (node.getValue() instanceof Symbol.Terminal(var s)) {
            return s;
        } else throw new RuntimeException("Unexpected Nonterminal: " + node.getValue());
    }

    private static Expression parsePrimary(List<Node<Symbol>> children) {
        Node<Symbol> firstChild = children.getFirst();
        Symbol first = firstChild.getValue();
        if (first instanceof Symbol.NonTerminal(var name)) {
            if (name.equals("Identifier")) {
                return Expression.nonTerminal(getTerminalChild(firstChild));
            } else if (name.equals("Literal") || name.equals("Class")) {
                String terminal = getTerminalChild(firstChild);
                String removeQuotes = terminal.replace("\"", "");
                return Expression.terminal(removeQuotes);
            } else {
                throw new RuntimeException("Unrecognized Child " + name);
            }
        } else if (first instanceof Symbol.Terminal(var firstTerminal)) {
            if (firstTerminal.equals(".")) {
                return Expression.empty();
            } else if (firstTerminal.equals("(")) {
                return Expression.group(parseExpressionRule(children.get(1).getChildren()));
            } else {
                throw new RuntimeException("Should have used switch case");
            }

        } else {
            throw new RuntimeException("Should have used switch case");
        }
    }

    private static String getTerminalChild(Node<Symbol> identifierRule) {
        Symbol value = identifierRule.getChildren().getFirst().getValue();
        if (value instanceof Symbol.Terminal(var name)) {
            return name;
        }
        throw new RuntimeException("Unexpected Nonterminal in Grammar Definition");
    }


    public Grammar(String startingRule, Map<String, Expression> rules) {
        Check.requireNotNull("The parameters to Grammar can't be null.",
                startingRule, rules);
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