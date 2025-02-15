package de.flogehring.jetpack.parse;

import de.flogehring.jetpack.util.Check;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Stack<T> {

    private ArrayList<T> container;

    private Stack() {
        container = new ArrayList<>();
    }

    public static <T> Stack<T> empty() {
        return new Stack<>();
    }

    public void push(T value) {
        container.add(value);
    }

    public T pop() {
        Check.require(!container.isEmpty(), "Can't pop empty stack");
        return container.removeLast();
    }

    public Set<T> getTopUntil(T value) {
        Check.require(container.contains(value), "Stack does not contain " + value);
        Set<T> result = new HashSet<>();
        for (var v : container.reversed()) {
            if (v.equals(value)) break;
            result.add(v);
        }
        return result;
    }
}
