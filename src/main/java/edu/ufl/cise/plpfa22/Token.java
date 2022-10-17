package edu.ufl.cise.plpfa22;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;

public class Token implements IToken {

    public static final char[] ESCAPED_SYMBOLS = {'b', 't', 'n', 'f', 'r', '"', '\'', '\\'};
    private final Kind kind;
    private char[] text;
    private final SourceLocation sourceLocation;

    private Integer intValue;
    private Boolean boolValue;
    private String stringValue;

    public Token(Kind kind, char[] text, SourceLocation sourceLocation) {
        this.kind = kind;
        this.text = text;
        this.sourceLocation = sourceLocation;

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

    public void setText(char[] text) {
        this.text = text;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public int getIntValue() {
        throwIfInvalidKind(getKind(), NUM_LIT);
        if (intValue == null) {
            intValue = Integer.parseInt(new String(getText()));
        }
        return intValue;
    }

    @Override
    public boolean getBooleanValue() {
        throwIfInvalidKind(getKind(), BOOLEAN_LIT);
        if (boolValue == null) {
            boolValue = Boolean.parseBoolean(new String(getText()));
        }
        return boolValue;
    }

    @Override
    public String getStringValue() {
        throwIfInvalidKind(getKind(), STRING_LIT, IDENT);
        if (stringValue == null) {
            final StringBuilder joined = new StringBuilder();
            final int num = ESCAPED_SYMBOLS.length;
            for (int i = 0; i < num; i++) {
                if (ESCAPED_SYMBOLS[i] == '\\') joined.append("\\\\");
                else joined.append(ESCAPED_SYMBOLS[i]);
            }
            String text = new String(getText());
            final Pattern quotesPattern = Pattern.compile("\"(.*)\"", Pattern.DOTALL);
            final Matcher quotesMatcher = quotesPattern.matcher(text);
            text = quotesMatcher.replaceAll("$1");

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

    private static void throwIfInvalidKind(final Kind is, final Kind... expected) {
        final List<Kind> expectedKinds = Arrays.asList(expected);
        if (!expectedKinds.contains(is)) {
            throw new RuntimeException("Token is not of type : " + expectedKinds);
        }
    }
}
