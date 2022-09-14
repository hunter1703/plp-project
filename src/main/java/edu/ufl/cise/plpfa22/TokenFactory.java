package edu.ufl.cise.plpfa22;

import static edu.ufl.cise.plpfa22.IToken.Kind.STRING_LIT;

public final class TokenFactory {
    private TokenFactory(){
    }

    public static Token ofKind(final IToken.Kind kind, final String text, final IToken.SourceLocation location) {
        if (kind == STRING_LIT) {
            return new StringToken(text, location);
        }
        return new Token(kind, text.toCharArray(), location);
    }
}
