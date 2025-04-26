package de.flogehring.jetpack.datatypes;

public sealed interface MemoTableLookup<T> {

    record NoHit<T>() implements MemoTableLookup<T> {
    }

    record Hit<T>(T value) implements MemoTableLookup<T> {
    }
}
