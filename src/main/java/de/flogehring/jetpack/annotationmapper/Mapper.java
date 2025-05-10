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

    private final PrimitiveMapper primitiveMapper;

    private Mapper(PrimitiveMapper primitiveMapper) {
        this.primitiveMapper = primitiveMapper;
    }

    public static Mapper defaultMapper() {
        return new Mapper(
                new DefaultPrimitiveMapper()
        );
    }

    public <T> T map(Node<Symbol> node, Class<T> clazz) throws Exception {
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

    private <T> T mapConcreteClass(Node<Symbol> node, Class<T> clazz, String name) throws Exception {
        T instance;
        instance = clazz.getConstructor().newInstance();
        FromRule rule = clazz.getAnnotation(FromRule.class);
        if (rule == null || !rule.value().equals(name)) {
            throw new IllegalArgumentException("No matching rule found for class " + clazz.getSimpleName());
        }
        for (Field field : clazz.getDeclaredFields()) {
            FromChild childAnnotation = field.getAnnotation(FromChild.class);
            if (childAnnotation != null) {
                Node<Symbol> childNode = node.getChildren().get(childAnnotation.index());
                Object valueForField = mapValue(field, childNode);
                field.set(instance, valueForField);
            }
        }
        return instance;
    }

    private Object mapValue(Field field, Node<Symbol> childNode) throws Exception {
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

    private List<Object> parseList(Field field, Node<Symbol> childNode) throws Exception {
        List<Object> list = new ArrayList<>();
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        Class<?> listElementType = (Class<?>) listType.getActualTypeArguments()[0];
        for (Node<Symbol> grandChild : childNode.getChildren()) {
            list.add(map(grandChild, listElementType));
        }
        return list;
    }

    private <T> Object getPrimitiveValue(Class<T> fieldType, String terminalValue) {
        if (fieldType.equals(Integer.TYPE)) {
            return primitiveMapper.mapInt(terminalValue);
        } else if (fieldType.equals(Byte.TYPE)) {
            return primitiveMapper.mapByte(terminalValue);
        } else if (fieldType.equals(Boolean.TYPE)) {
            return primitiveMapper.mapBoolean(terminalValue);
        } else if (fieldType.equals(Character.TYPE)) {
            return primitiveMapper.mapChar(terminalValue);
        } else if (fieldType.equals(Long.TYPE)) {
            return primitiveMapper.mapLong(terminalValue);
        } else if (fieldType.equals(Float.TYPE)) {
            return primitiveMapper.mapFloat(terminalValue);
        } else if (fieldType.equals(Short.TYPE)) {
            return primitiveMapper.mapShort(terminalValue);
        } else if (fieldType.equals(Double.TYPE)) {
            return primitiveMapper.mapDouble(terminalValue);
        } else {
            throw new RuntimeException("Error parsing type " + fieldType + " as primitive value");
        }
    }

    private <T> T mapInterface(Node<Symbol> node, Class<T> clazz) throws Exception {
        Node<Symbol> child = Check.requireSingleItem(
                node.getChildren(),
                "Interface Annotated with from Rule " + clazz.getSimpleName()
        );
        if (child.getValue() instanceof Symbol.NonTerminal(var rule)) {
            Class<Object> subclassForRule = getSubclassForInterface(clazz, rule);
            return (T) map(child, subclassForRule);
        } else {
            throw new RuntimeException("Cant parse abstract class from terminal node");
        }
    }

    private static Class<Object> getSubclassForInterface(Class<?> clazz, String rule) {
        Delegate[] annotations = clazz.getAnnotationsByType(Delegate.class);
        List<Class<Object>> delegateClasses = resolveDelegates(annotations);
        List<Class<Object>> list = delegateClasses.stream()
                .filter(delegateClass ->
                        delegateClass.getAnnotation(FromRule.class).value()
                                .equals(rule))
                .toList();
        return Check.requireSingleItem(
                list,
                MessageFormat.format(
                        "Found not exactly one matching class for rule {0} extending from {1}",
                        rule,
                        clazz.getName()
                )
        );
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

    private String getTerminalValue(Node<Symbol> childNode) {
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