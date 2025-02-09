package de.flogehring.jetpack.parse;

public sealed interface MemoTableLookup {

    record NoHit() implements MemoTableLookup {

    }

    record PreviousParsingFailure() implements MemoTableLookup {

    }

    record Success(int offset) implements MemoTableLookup {
    }

    record LeftRecursion(Result result) implements MemoTableLookup {

      public   sealed interface Result {
            record SeedParse(int offset) implements Result {
            }

            record Fail() implements Result {
            }
        }
    }

}
