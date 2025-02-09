package de.flogehring.jetpack.parse;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Explanation of Name, see
 * <a href="https://web.cs.ucla.edu/~todd/research/pepm08.pdf">Left Recursion Paper</a>
 */
public class Heads {

    private final HashMap<Integer, Head> heads;

    private Heads() {
        this.heads = new HashMap<>();
    }

    public static Heads of() {
        return new Heads();
    }

    public Optional<Head> get(int position) {
        if (heads.containsKey(position)) {
            return Optional.of(heads.get(position));
        }
        return Optional.empty();
    }

    public void remove(int position) {
        heads.remove(position);
    }

    public record Head(String rule, Set<String> rules) {

    }

}
