package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.grammar.ConsumedExpression;
import de.flogehring.jetpack.grammar.Symbol;

import java.util.Optional;

public sealed interface MemoTableLookup {

    record NoHit() implements MemoTableLookup {
    }

    record LeftRecursion(
            ConsumedExpression seed,
            Symbol.NonTerminal rule,
            Heads.Head head,
            Optional<LeftRecursion> leftRecursion
    ) implements MemoTableLookup {
    }

    record Fail() implements MemoTableLookup {
    }

    record Success(int offset) implements MemoTableLookup {
    }
}
