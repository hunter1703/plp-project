package edu.ufl.cise.plpfa22;

public class Token implements IToken {

    private final Kind kind;
    private final char[] text;
    private final SourceLocation sourceLocation;
    private final int length;

    private Integer intValue;
    private Boolean boolValue;
    private final String stringValue;

    public Token(Kind kind, char[] text, SourceLocation sourceLocation, String stringValue, int length) {
        if (kind == null) {
            throw new IllegalArgumentException("Kind cannot be null");
        }
        this.kind = kind;
        this.text = text;
        this.sourceLocation = sourceLocation;
        this.length = length;

        this.intValue = null;
        this.boolValue = null;
        this.stringValue = stringValue;
    }

    public static Token ofKind(final Kind kind, final String text, final SourceLocation location) {
        return new Token(kind, text.toCharArray(), location, text, text.length());
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public char[] getText() {
        return text;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public int getIntValue() {
        if (intValue == null) {
            intValue = Integer.parseInt(getStringValue());
        }
        return intValue;
    }

    @Override
    public boolean getBooleanValue() {
        if (boolValue == null) {
            boolValue = Boolean.parseBoolean(getStringValue());
        }
        return boolValue;
    }

    @Override
    public String getStringValue() {
        return stringValue;
    }

    @Override
    public int length() {
        return length;
    }
}
