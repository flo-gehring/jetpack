package de.friendlyhedgehog.jetpack.parse;

import de.friendlyhedgehog.jetpack.datatypes.Node;
import de.friendlyhedgehog.jetpack.grammar.Symbol;

import java.util.List;

 record ConsumedExpression(
        int parsePosition,
        List<Node<Symbol>> parseTree
) {
}
