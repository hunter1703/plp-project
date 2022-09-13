package edu.ufl.cise.plpfa22;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.ufl.cise.plpfa22.IToken.Kind.STRING_LIT;

public class StringToken extends Token {
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

    private final int numNewLines;
    private final int lastLineLen;

    public StringToken(final String rawText, final SourceLocation location) {
        super(STRING_LIT, rawText.toCharArray(), location, rawText.length());

        final Matcher matcher = NEW_LINE_PATTERN.matcher(getStringValue());
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        this.numNewLines = count;
        final int lastIndex = rawText.lastIndexOf("\n");
        this.lastLineLen = rawText.length() - (lastIndex >= 0 ? lastIndex + 1 : 0);
    }

    public int getNumNewLines() {
        return numNewLines;
    }

    public int getLastLineLen() {
        return lastLineLen;
    }
}
