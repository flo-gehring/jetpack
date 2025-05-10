package de.flogehring.jetpack.annotationmapper;

import de.flogehring.jetpack.datatypes.Node;
import de.flogehring.jetpack.grammar.Symbol;
import de.flogehring.jetpack.util.Check;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class Mapper {

    public static <T> T map(Node<Symbol> node, Class<T> clazz) throws Exception {
        Symbol symbol = node.getValue();
        if (symbol instanceof Symbol.NonTerminal(var name)) {
            T instance;
            if (clazz.isInterface()) {
                instance = mapInterface(node, clazz);
            } else if (clazz.isEnum()) {
                String s = getTerminalValue(node);
                instance = Arrays.stream(clazz.getEnumConstants())
                        .filter(enumConstant -> enumConstant.toString().equals(s))
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("Can't find ENUM Constant " + s + " for " + clazz));
            } else if (clazz.isPrimitive()) {
                instance = (T) getPrimitiveValue(clazz, getTerminalValue(node));
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
            FromChild childAnnotation = field.getAnnotation(FromChild.class);
            if (childAnnotation != null) {
                Node<Symbol> childNode = node.getChildren().get(childAnnotation.index());
                Object valueForField = mapValue(field, childNode);
                field.set(instance, valueForField);
            }
        }
        return instance;
    }

    private static Object mapValue(Field field, Node<Symbol> childNode) throws Exception {
        Object result;
        Class<?> fieldType = field.getType();
        if (fieldType.equals(String.class)) {
            result = getTerminalValue(childNode);
        } else if (fieldType.isEnum()) {
            result = fieldType.getMethod("valueOf", String.class)
                    .invoke(null, getTerminalValue(childNode));
        } else if (fieldType.equals(Integer.class)) {
            result = Integer.parseInt(getTerminalValue(childNode));
        } else if (fieldType.isPrimitive()) {
            result = getPrimitiveValue(fieldType, getTerminalValue(childNode));
        } else if (fieldType.isAnnotationPresent(FromRule.class)) {
            result = map(childNode, fieldType);
        } else if (List.class.isAssignableFrom(fieldType)) {
            result = parseList(field, childNode);
        } else if (fieldType.isInterface()) {
            result = map(childNode, fieldType);
        } else {
            throw new RuntimeException("Could not get value for field " + field);
        }
        return result;
    }

    private static List<Object> parseList(Field field, Node<Symbol> childNode) throws Exception {
        List<Object> list = new ArrayList<>();
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        Class<?> listElementType = (Class<?>) listType.getActualTypeArguments()[0];
        for (Node<Symbol> grandChild : childNode.getChildren()) {
            list.add(map(grandChild, listElementType));
        }
        return list;
    }

    private static <T> Object getPrimitiveValue(Class<T> fieldType, String terminalValue) {
        if (fieldType.equals(Integer.TYPE)) {
            return Integer.parseInt(terminalValue);
        } else if (fieldType.equals(Byte.TYPE)) {
            return Byte.parseByte(terminalValue, 8);
        } else if (fieldType.equals(Boolean.TYPE)) {
            // TODO Attach Meta-Info to custom parse True and False
            return Boolean.parseBoolean(terminalValue);
        } else if (fieldType.equals(Character.TYPE)) {
            // TODO Parse char Values
            throw new RuntimeException("Can't parse Char values yet");
        } else if (fieldType.equals(Long.TYPE)) {
            return Long.valueOf(terminalValue);
        } else if (fieldType.equals(Float.TYPE)) {
            return Float.parseFloat(terminalValue);
        } else if (fieldType.equals(Short.TYPE)) {
            return Short.valueOf(terminalValue);
        } else if (fieldType.equals(Double.TYPE)) {
            // TODO Attach Meta-Info to parse Numbers by a custom format
            return Double.parseDouble(terminalValue);
        } else {
            throw new RuntimeException("Error parsing type " + fieldType + " as primitive value");
        }
    }

    private static <T> T mapInterface(Node<Symbol> node, Class<T> clazz) throws Exception {
        Delegate[] annotations = clazz.getAnnotationsByType(Delegate.class);
        Node<Symbol> child = Check.requireSingleItem(
                node.getChildren(),
                "Interface Annotated with from Rule " + clazz.getSimpleName()
        );
        if (child.getValue() instanceof Symbol.NonTerminal(var nameChild)) {
            List<Class<Object>> delegateClasses = resolveDelegates(annotations);
            List<Class<Object>> list = delegateClasses.stream()
                    .filter(delegateClass -> delegateClass.getAnnotation(FromRule.class).value().equals(nameChild))
                    .toList();
            Class<Object> subclassForRule = Check.requireSingleItem(
                    list,
                    MessageFormat.format("Found not exactly one matching class for rule {0} extending from {1}",
                            nameChild, clazz.getName())
            );
            return (T) map(child, subclassForRule);
        } else {
            throw new RuntimeException("Cant parse abstract class from terminal node");
        }
    }

    private static List<Class<Object>> resolveDelegates(Delegate[] annotations) {
        List<Class<Object>> classes = Arrays.stream(annotations).
                map(delegate -> (Class<Object>) delegate.clazz())
                .toList();
        List<Delegate> delegateAnnotationsOfTransitveClasses = Arrays.stream(annotations)
                .filter(Delegate::transitive)
                .map(Delegate::clazz)
                .flatMap(clazz -> Arrays.stream(clazz.getAnnotationsByType(Delegate.class)))
                .toList();
        List<Class<Object>> transitive;
        if (!delegateAnnotationsOfTransitveClasses.isEmpty()) {
            transitive = resolveDelegates(
                    delegateAnnotationsOfTransitveClasses.toArray(Delegate[]::new)
            );
        } else {
            transitive = List.of();
        }
        return Stream.concat(classes.stream(), transitive.stream()).toList();
    }

    private static String getTerminalValue(Node<Symbol> childNode) {
        if (childNode.getValue() instanceof Symbol.Terminal(var text)) {
            return text;
        } else {
            List<Node<Symbol>> grandChildren = childNode.getChildren();
            Node<Symbol> grandChild = Check.requireSingleItem(
                    grandChildren,
                    "Expected either Terminal value or node with single child"
            );
            if (grandChild.getValue() instanceof Symbol.Terminal(var text)) {
                return text;
            }
            throw new RuntimeException("Expected either Child to be terminal or have one terminal grandchild, got: " + childNode);
        }
    }
}