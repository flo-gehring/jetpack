package de.flogehring.jetpack.datatypes;

public sealed interface Either<T, S> {

    T getEither();
    S getOr();

    static <T,S> Either<T,S> ofThis(T t) {
        return new This<>(t);
    }

    static <T,S> Either<T,S> or(S s) {
        return new Or<>(s);
    }
    record This<T, S>(T t) implements Either<T, S> {
        public T get() {
            return t;
        }

        @Override
        public T getEither() {
            return get();
        }

        @Override
        public S getOr() {
            throw new RuntimeException();
        }
    }

    record Or<T, S>(S s) implements Either<T, S> {
        @Override
        public T getEither() {
            throw new RuntimeException("");
        }

        @Override
        public S getOr() {
            return s;
        }
    }
}
