package edu.ufl.cise.plpfa22;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;

public class Token implements IToken {

    public static final char[] ESCAPED_SYMBOLS = {'b', 't', 'n', 'f', 'r', '"', '\'', '\\'};
    private final Kind kind;
    private final char[] text;
    private final SourceLocation sourceLocation;
    private final int length;

    private Integer intValue;
    private Boolean boolValue;
    private String stringValue;

    public Token(Kind kind, char[] text, SourceLocation sourceLocation, int length) {
        if (kind == null) {
            throw new IllegalArgumentException("Kind cannot be null");
        }
        this.kind = kind;
        this.text = text;
        this.sourceLocation = sourceLocation;
        this.length = length;

        this.intValue = null;
        this.boolValue = null;
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
        throwIfInvalidKind(NUM_LIT, getKind());
        if (intValue == null) {
            intValue = Integer.parseInt(new String(getText()));
        }
        return intValue;
    }

    @Override
    public boolean getBooleanValue() {
        throwIfInvalidKind(BOOLEAN_LIT, getKind());
        if (boolValue == null) {
            boolValue = Boolean.parseBoolean(new String(getText()));
        }
        return boolValue;
    }

    @Override
    public String getStringValue() {
        throwIfInvalidKind(STRING_LIT, getKind());
        if (stringValue == null) {
            final StringBuilder joined = new StringBuilder();
            final int num = ESCAPED_SYMBOLS.length;
            for (int i = 0; i < num; i++) {
                if (ESCAPED_SYMBOLS[i] == '\\') joined.append("\\\\");
                else joined.append(ESCAPED_SYMBOLS[i]);
            }
            String text = new String(getText());
            final Pattern quotesPattern = Pattern.compile("(?<!\\\\)\\\"", Pattern.DOTALL);
            final Matcher quotesMatcher = quotesPattern.matcher(text);
            text = quotesMatcher.replaceAll("");

            final Pattern pattern = Pattern.compile("\\\\[" + joined + "]", Pattern.DOTALL);
            final Matcher matcher = pattern.matcher(text);
            final StringBuilder escapeReplacedString = new StringBuilder();
            while (matcher.find()) {
                final String escapeCharString = matcher.group();
                switch (escapeCharString) {
                    case "\\b":
                        matcher.appendReplacement(escapeReplacedString, Character.toString('\b'));
                        break;
                    case "\\t":
                        matcher.appendReplacement(escapeReplacedString, Character.toString('\t'));
                        break;
                    case "\\n":
                        matcher.appendReplacement(escapeReplacedString, Character.toString('\n'));
                        break;
                    case "\\f":
                        matcher.appendReplacement(escapeReplacedString, Character.toString('\f'));
                        break;
                    case "\\r":
                        matcher.appendReplacement(escapeReplacedString, Character.toString('\r'));
                        break;
                    case "\\\"":
                        matcher.appendReplacement(escapeReplacedString, Character.toString('"'));
                        break;
                    case "\\'":
                        matcher.appendReplacement(escapeReplacedString, Character.toString('\''));
                        break;
                    case "\\\\":
                        matcher.appendReplacement(escapeReplacedString, "\\\\");
                        break;
                    default:
                        System.out.println(escapeCharString);
                }
            }
            matcher.appendTail(escapeReplacedString);
            stringValue = escapeReplacedString.toString();
        }
        return stringValue;
    }

    @Override
    public int length() {
        return length;
    }

    private static void throwIfInvalidKind(final Kind expected, final Kind is) {
        if (is != expected) {
            throw new RuntimeException("Token is not of type : " + expected);
        }
    }
}
