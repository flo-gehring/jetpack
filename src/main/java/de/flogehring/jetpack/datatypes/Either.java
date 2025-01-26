package de.flogehring.jetpack.datatypes;

public sealed interface Either<T, S> {

    static <T,S> Either<T,S> ofThis(T t) {
        return new This<>(t);
    }

    static <T,S> Either<T,S> or(S s) {
        return new Or<>(s);
    }
    record This<T, S>(T t) implements Either<T, S> {
    }

    record Or<T, S>(S s) implements Either<T, S> {
    }
}
