package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.List;

 record ConsumedExpression(
        int parsePosition,
        List<Node<Symbol>> parseTree
) {
}
