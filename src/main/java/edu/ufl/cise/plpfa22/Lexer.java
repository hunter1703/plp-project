package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.IToken.SourceLocation;

import java.util.*;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;

public class Lexer implements ILexer {
    private static final Map<String, Integer> KIND_VS_PRIORITY = new HashMap<>();
    private static final Token NEW_LINE_TOKEN = TokenFactory.ofKind(null, "", null);
    private static final Token EXT_NEW_LINE_TOKEN = TokenFactory.ofKind(null, "", null);
    private static final Token WHITE_SPACE_TOKEN = TokenFactory.ofKind(null, "", null);
    private static final Token COMMENT_TOKEN = TokenFactory.ofKind(null, "", null);

    static {
        final String[] allKinds = new String[]{EOF.name(), "COMMENT", "NEW_LINE", "WHITE_SPACE", ERROR.name(), DOT.name(), COMMA.name(), SEMI.name(), LPAREN.name(), RPAREN.name(), PLUS.name(), MINUS.name(), TIMES.name(), DIV.name(), MOD.name(), QUESTION.name(), BANG.name(), ASSIGN.name(), EQ.name(), NEQ.name(), LT.name(), LE.name(), GT.name(), GE.name(), KW_CONST.name(), KW_VAR.name(), KW_PROCEDURE.name(), KW_CALL.name(), KW_BEGIN.name(), KW_END.name(), KW_IF.name(), KW_THEN.name(), KW_WHILE.name(), KW_DO.name(), BOOLEAN_LIT.name(), NUM_LIT.name(), IDENT.name(), STRING_LIT.name()};
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

    private int peekIndex;
    private int peekLine;
    private int peekColumn;

    public Lexer(String input, FSA fsa) {
        this.input = input == null ? new char[0] : input.toCharArray();
        this.fsa = fsa;
        this.currIndex = this.peekIndex = 0;
        this.tokenLine = this.peekLine = 1;
        this.tokenColumn = this.peekColumn = 1;
    }

    @Override
    public IToken next() throws LexicalException {
        final IToken nextToken = peek();
        currIndex = peekIndex;
        tokenLine = peekLine;
        tokenColumn = peekColumn;
        return nextToken;
    }

    @Override
    public IToken peek() throws LexicalException {
        IToken nextToken = null;
        peekIndex = currIndex;
        peekLine = tokenLine;
        peekColumn = tokenColumn;

        while (nextToken == null || nextToken == NEW_LINE_TOKEN || nextToken == EXT_NEW_LINE_TOKEN || nextToken == WHITE_SPACE_TOKEN || nextToken == COMMENT_TOKEN) {
            nextToken = peek(peekIndex, peekLine, peekColumn);
            final Kind kind = nextToken.getKind();
            int len = -1;
            if (nextToken == NEW_LINE_TOKEN || nextToken == EXT_NEW_LINE_TOKEN) {
                peekLine++;
                peekColumn = 1;
                len = nextToken == NEW_LINE_TOKEN ? 1 : 2;
            } else if (nextToken == COMMENT_TOKEN) {
                peekLine++;
                peekColumn = 1;
                len = nextToken.getText().length;
            } else if (nextToken == WHITE_SPACE_TOKEN) {
                peekColumn++;
                len = 1;
            } else if (kind == STRING_LIT) {
                //see testStringLineNum()
                final StringToken stringToken = (StringToken) nextToken;
                final int numNewLines = stringToken.getNumNewLines();
                peekLine += numNewLines;
                if (numNewLines > 0) {
                    peekColumn = 1;
                }
                peekColumn += stringToken.getLastLineLen();
                len = nextToken.getText().length;
            } else {
                len = nextToken.getText().length;
                peekColumn += len;
            }
            peekIndex += len;
        }
        return nextToken;
    }

    public IToken peek(int index, final int line, final int column) throws LexicalException {
        try {
            final int length = input.length;
            if (index >= length) {
                return TokenFactory.ofKind(EOF, "\0", null);
            }
            final StringBuilder sb = new StringBuilder();

            Token possibleToken = null;

            //consume as many characters as you can
            while (index < length) {
                final char ch = input[index++];
                sb.append(ch);
                final List<String> longerTokenKinds = new ArrayList<>(fsa.advance(ch));
                //FSA did not advance to new states
                if (!fsa.advanced()) {
                    if (possibleToken == null) {
                        throw new LexicalException();
                    }
                    //return longest token recognized yet
                    int len = -1;
                    if (possibleToken.getKind() == NUM_LIT) {
                        possibleToken.getIntValue();
                    }
                    return possibleToken;
                } else if (!longerTokenKinds.isEmpty()) {
                    //longer tokens recognized; spit out tokens based on priority (e.g. keywords are higher priority than identifiers)
                    final String kind = getHighestPriority(longerTokenKinds);
                    if ("NEW_LINE".equals(kind)) {
                        possibleToken = sb.length() == 1 ? NEW_LINE_TOKEN : EXT_NEW_LINE_TOKEN;
                    } else if ("WHITE_SPACE".equals(kind)) {
                        possibleToken = WHITE_SPACE_TOKEN;
                    } else if ("COMMENT".equals(kind)) {
                        possibleToken = COMMENT_TOKEN;
                        possibleToken.setText(sb.toString().toCharArray());
                    } else {
                        possibleToken = TokenFactory.ofKind(Kind.valueOf(kind), sb.toString(), new SourceLocation(line, column));
                    }
                }
            }
            if (possibleToken == null) {
                throw new LexicalException();
            }
            return possibleToken;
        } catch (NumberFormatException ex) {
            throw new LexicalException("Invalid integer");
        } finally {
            fsa.reset();
        }
    }

    private static String getHighestPriority(final List<String> allKinds) {
        allKinds.sort(Comparator.comparingInt(KIND_VS_PRIORITY::get));
        return allKinds.get(0);
    }
}
