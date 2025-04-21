package de.flogehring.jetpack.construction_annotation;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.MessageFormat;

public class Mapper {

    public static <T> T map(Node<Symbol> node, Class<T> clazz) throws Exception {
        Symbol symbol = node.getValue();

        if (symbol instanceof Symbol.NonTerminal(var name)) {
            T instance;
            if (clazz.isInterface()) {
                instance = mapInterface(node, clazz);
            } else {
                instance = mapConcreteClass(node, clazz, name);
            }
            return instance;
        } else {
            throw new RuntimeException("Can't start mapping on Terminal Value");
        }
    }

    private static <T> T mapConcreteClass(Node<Symbol> node, Class<T> clazz, String name) throws Exception {
        T instance;
        instance = clazz.getConstructor().newInstance();
        FromRule rule = clazz.getAnnotation(FromRule.class);
        if (rule == null || !rule.value().equals(name)) {
            throw new IllegalArgumentException("No matching rule found for class " + clazz.getSimpleName());
        }
        for (Field field : clazz.getFields()) {
            FromChild childAnno = field.getAnnotation(FromChild.class);
            if (childAnno != null) {
                Node<Symbol> childNode = node.getChildren().get(childAnno.index());
                Class<?> fieldType = field.getType();
                if (fieldType.equals(String.class)) {
                    field.set(instance, getTerminalValue(childNode));
                } else if (fieldType.isEnum()) {
                    field.set(
                            instance,
                            fieldType.getMethod("valueOf", String.class)
                                    .invoke(null, getTerminalValue(childNode))
                    );
                } else if (fieldType.equals(Integer.class)) {
                    field.set(instance,
                            Integer.parseInt(getTerminalValue(childNode))
                    );
                } else if (fieldType.isAnnotationPresent(FromRule.class)) {
                    Object nested = map(childNode, fieldType);
                    field.set(instance, nested);
                } else if (List.class.isAssignableFrom(fieldType)) {
                    Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    Class<?> listElementType = (Class<?>) genericType;
                    List<Object> list = new ArrayList<>();
                    for (Node<Symbol> grandChild : childNode.getChildren()) {
                        if (listElementType.equals(String.class)) {
                            list.add(getTerminalValue(grandChild));
                        } else if (listElementType.isAnnotationPresent(FromRule.class)) {
                            list.add(map(grandChild, listElementType));
                        }
                    }
                    field.set(instance, list);
                }
            }
        }
        return instance;
    }

    private static <T> T mapInterface(Node<Symbol> node, Class<T> clazz) throws Exception {
        OnRule[] annotations = clazz.getAnnotationsByType(OnRule.class);
        Node<Symbol> child = Check.requireSingleItem(node.getChildren(), "Interface Annotated with from Rule " + clazz.getSimpleName());
        if (child.getValue() instanceof Symbol.NonTerminal(var nameChild)) {
            OnRule ruleForSubclass = Arrays.stream(annotations).
                    filter(onRule -> onRule.rule().equals(nameChild))
                    .findFirst()
                    .orElseThrow();
            return (T) map(child, ruleForSubclass.clazz());
        } else {
            throw new RuntimeException("Cant parse abstract class from terminal node");
        }
    }

    private static String getTerminalValue(Node<Symbol> childNode) {
        if (childNode.getValue() instanceof Symbol.Terminal(var text)) {
            return text;
        } else {
            throw new RuntimeException(MessageFormat.format(
                    "Expected Terminal value, got Non-Terminal-Rule {0}",
                    ((Symbol.NonTerminal) childNode.getValue()).name()
            ));
        }
    }
}