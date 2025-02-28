package de.flogehring.jetpack.datatypes;

import de.flogehring.jetpack.grammar.Grammar;

import java.util.function.Function;

public sealed interface Either<T, S> {

    T getEither();

    S getOr();

    static <T, S> Either<T, S> ofThis(T t) {
        return new This<>(t);
    }

    static <T, S> Either<T, S> or(S s) {
        return new Or<>(s);
    }

    <U> Either<U, S> map(Function<T, U> f);

    <U> Either<U, S> flatMap(Function<T, Either<U, S>> f);

    record This<T, S>(T t) implements Either<T, S> {

        public T get() {
            return t;
        }

        @Override
        public T getEither() {
            return get();
        }

        @Override
        public <U> Either<U, S> map(Function<T, U> f) {
            return new This<>(f.apply(get()));
        }

        @Override
        public S getOr() {
            throw new RuntimeException();
        }

        @Override
        public <U> Either<U, S> flatMap(Function<T, Either<U, S>> f) {
            return f.apply(t);
        }
    }

    record Or<T, S>(S s) implements Either<T, S> {

        @Override
        public T getEither() {
            throw new RuntimeException("Tried to get Either bot was Or");
        }

        @Override
        public S getOr() {
            return s;
        }

        @Override
        public <U> Either<U, S> map(Function<T, U> f) {
            return Either.or(s);
        }

        @Override
        public <U> Either<U, S> flatMap(Function<T, Either<U, S>> f) {
            return Either.or(s);
        }
    }
}
