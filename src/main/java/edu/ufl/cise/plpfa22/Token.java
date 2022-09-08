package edu.ufl.cise.plpfa22;

import java.util.HashMap;
import java.util.Map;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;

public class Token implements IToken {
//    private static final Map<String, Kind> STRING_VS_KIND = new HashMap<>();
//
//    //	        ERROR, // use to avoid exceptions if scanning all input at once
//    static {
//        STRING_VS_KIND.put(null, EOF);
//        STRING_VS_KIND.put(".", DOT);
//        STRING_VS_KIND.put(",", COMMA);
//        STRING_VS_KIND.put(";", SEMI);
//        STRING_VS_KIND.put("\"", QUOTE);
//        STRING_VS_KIND.put("(", LPAREN);
//        STRING_VS_KIND.put(")", RPAREN);
//        STRING_VS_KIND.put("+", PLUS);
//        STRING_VS_KIND.put("-", MINUS);
//        STRING_VS_KIND.put("*", TIMES);
//        STRING_VS_KIND.put("/", DIV);
//        STRING_VS_KIND.put("%", MOD);
//        STRING_VS_KIND.put("?", QUESTION);
//        STRING_VS_KIND.put("!", BANG);
//        STRING_VS_KIND.put("WHILE", KW_WHILE);
//        STRING_VS_KIND.put("THEN", KW_THEN);
//        STRING_VS_KIND.put("IF", KW_IF);
//        STRING_VS_KIND.put("END", KW_END);
//        STRING_VS_KIND.put("BEGIN", KW_BEGIN);
//        STRING_VS_KIND.put("CALL", KW_CALL);
//        STRING_VS_KIND.put("PROCEDURE", KW_PROCEDURE);
//        STRING_VS_KIND.put("VAR", KW_VAR);
//        STRING_VS_KIND.put("CONST", KW_CONST);
//        STRING_VS_KIND.put(">=", GE);
//        STRING_VS_KIND.put(">", GT);
//        STRING_VS_KIND.put("<=", LE);
//        STRING_VS_KIND.put("<", LT);
//        STRING_VS_KIND.put("#", NEQ);
//        STRING_VS_KIND.put("=", EQ);
//        STRING_VS_KIND.put(":=", ASSIGN);
//    }

    private final Kind kind;
    private final char[] text;
    private final SourceLocation sourceLocation;
    private final int length;

    private Integer intValue;
    private Boolean boolValue;
    private String stringValue;

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

//    public static Token ofInt(final String text, final SourceLocation location) {
//        return new Token(NUM_LIT, text.toCharArray(), location, Integer.parseInt(text), false, null, text.length());
//    }
//
//    public static Token ofBool(final String text, final SourceLocation location) {
//        return new Token(BOOLEAN_LIT, text.toCharArray(), location, 0, Boolean.parseBoolean(text), null, text.length());
//    }
//
//    public static Token ofString(final String text, final SourceLocation location) {
//        return new Token(STRING_LIT, text.toCharArray(), location, 0, false, text, text.length());
//    }
//
//    public static Token ofIdentifier(final String text, final SourceLocation location) {
//        return new Token(IDENT, text.toCharArray(), location, 0, false, text, text.length());
//    }

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
