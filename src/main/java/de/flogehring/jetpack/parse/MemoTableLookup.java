package de.flogehring.jetpack.parse;

public sealed interface MemoTableLookup {

    record NoHit() implements MemoTableLookup {
    }

    record Fail() implements MemoTableLookup {
    }

    record Success(int offset) implements MemoTableLookup {
    }
}
