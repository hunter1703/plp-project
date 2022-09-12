package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.IToken.SourceLocation;

import java.util.*;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;

public class Lexer implements ILexer {
    private static final Map<Kind, Integer> KIND_VS_PRIORITY = new HashMap<>();

    static {
        final Kind[] allKinds = new Kind[]{EOF, COMMENT, NEW_LINE, WHITE_SPACE, ERROR, DOT, COMMA, SEMI, QUOTE, LPAREN, RPAREN, PLUS, MINUS, TIMES, DIV, MOD, QUESTION, BANG, ASSIGN, EQ, NEQ, LT, LE, GT, GE, KW_CONST, KW_VAR, KW_PROCEDURE, KW_CALL, KW_BEGIN, KW_END, KW_IF, KW_THEN, KW_WHILE, KW_DO, BOOLEAN_LIT, NUM_LIT, IDENT, STRING_LIT};
        final int num = allKinds.length;
        for (int i = 0; i < num; i++) {
            KIND_VS_PRIORITY.put(allKinds[i], i);
        }
    }

    private final char[] input;
    private final FSA fsa;
    private int currIndex;
    private int tokenLine;
    private int tokenColumn;

    public Lexer(String input, FSA fsa) {
        this.input = input == null ? new char[0] : input.toCharArray();
        this.fsa = fsa;
        this.currIndex = 0;
        this.tokenLine = 1;
        this.tokenColumn = 1;
    }

    @Override
    public IToken next() throws LexicalException {
        final IToken nextToken = peek();
        //advance
        final int len = nextToken.length();
        currIndex += len;

        final Kind kind = nextToken.getKind();
        if (kind == NEW_LINE) {
            tokenLine++;
            tokenColumn = 1;
        } else {
            tokenColumn += len;
        }

        if (kind == NEW_LINE || kind == WHITE_SPACE || kind == COMMENT) {
            return next();
        }
        return nextToken;
    }

    @Override
    public IToken peek() throws LexicalException {
        try {
            final int length = input.length;
            if (currIndex >= length) {
                return Token.ofKind(EOF, "\0", null);
            }
            int index = currIndex;
            final StringBuilder sb = new StringBuilder();

            Token possibleToken = null;

            //consume as many characters as you can
            while (index < length) {
                final char ch = input[index++];
                sb.append(ch);
                final List<Kind> longerTokenKinds = new ArrayList<>(fsa.advance(ch));
                //FSA did not advance to new states
                if (!fsa.advanced()) {
                    if (possibleToken == null) {
                        throw new LexicalException();
                    }
                    //return longest token recognized yet
                    if (possibleToken.getKind() == NUM_LIT) {
                        possibleToken.getIntValue();
                    }
                    return possibleToken;
                } else if (!longerTokenKinds.isEmpty()) {
                    //longer tokens recognized; spit out tokens based on priority (e.g. keywords are higher priority than identifiers)
                    final Kind kind = getHighestPriority(longerTokenKinds);
                    possibleToken = Token.ofKind(kind, sb.toString(), new SourceLocation(tokenLine, tokenColumn));
                }
            }
            return possibleToken;
        } catch (NumberFormatException ex) {
          throw new LexicalException("Invalid integer");
        } finally {
            fsa.reset();
        }
    }

    private static Kind getHighestPriority(final List<Kind> allKinds) {
        allKinds.sort(Comparator.comparingInt(KIND_VS_PRIORITY::get));
        return allKinds.get(0);
    }
}
