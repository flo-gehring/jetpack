package de.flogehring.jetpack.parse;

public sealed interface MemoTableLookup {

    record NoHit() implements MemoTableLookup {

    }

    record LeftRecursion(boolean detected) implements MemoTableLookup {


    }

    record PreviousParsingFailure() implements MemoTableLookup {

    }

    record Success(int offset) implements MemoTableLookup {
    }
}
