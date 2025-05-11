package de.friendlyhedgehog.jetpack.annotationmapper;

/// Simply delegates to the parse*Primititve* methods (e.g. [java.lang.Integer#parseInt])
/// for most of the primitive types.
/// Exceptions are
/// * [java.lang.Byte#parseByte]: Additionally,  a radix of 16 is specified.
/// * [java.lang.Character]: an exception is thrown if the string has length != 1 and if length = 1 then
///   [java.lang.String#charAt] is called.
public class DefaultPrimitiveMapper implements PrimitiveMapper {

    @Override
    public int mapInt(String s) {
        return Integer.parseInt(s);
    }

    @Override
    public byte mapByte(String s) {
        return Byte.parseByte(s, 16);
    }

    @Override
    public boolean mapBoolean(String s) {
        return Boolean.parseBoolean(s);
    }

    @Override
    public char mapChar(String s) {
        if (s.length() != 1) {
            throw new RuntimeException(
                    "Try to map char from string with length != 0: " + s
            );
        }
        return s.charAt(0);
    }

    @Override
    public long mapLong(String s) {
        return Long.parseLong(s);
    }

    @Override
    public float mapFloat(String s) {
        return Float.parseFloat(s);
    }

    @Override
    public short mapShort(String s) {
        return Short.parseShort(s);
    }

    @Override
    public double mapDouble(String s) {
        return Double.parseDouble(s);
    }
}