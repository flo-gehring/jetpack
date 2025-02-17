package de.flogehring.jetpack.datatypes;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
public class Node<S> {

    private final S value;
    @Getter
    private final List<Node<S>> children;

    private Node(S value, List<Node<S>> children) {
        this.value = value;
        this.children = children;
    }

    public static <S> Node<S> of(S value, List<Node<S>> children) {
        return new Node<>(value, children);
    }

    public static <S> Node<S> leaf(S value) {
        return new Node<>(value, new ArrayList<>());
    }
}
