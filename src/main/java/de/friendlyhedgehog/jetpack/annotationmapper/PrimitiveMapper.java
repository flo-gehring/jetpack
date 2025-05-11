package de.friendlyhedgehog.jetpack.annotationmapper;

/// Specifies how Java Primitive types should be parsed from
/// the Strings that are encountered in the language.
public interface PrimitiveMapper {

    int mapInt(String s);
    byte mapByte(String s);
    boolean mapBoolean(String s);
    char mapChar(String s);
    long mapLong(String s);
    float mapFloat(String s);
    short mapShort(String s);
    double mapDouble(String s);
}
