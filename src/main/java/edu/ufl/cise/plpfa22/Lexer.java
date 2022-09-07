package edu.ufl.cise.plpfa22;

public class Lexer implements ILexer {
    private final char[] input;
    private final FSA fsa;
    private int currIndex;

    public Lexer(String input, FSA fsa) {
        this.input = input == null ? new char[0] : input.toCharArray();
        this.fsa = fsa;
        this.currIndex = 0;
    }

    @Override
    public IToken next() throws LexicalException {
        final IToken nextToken = peek();
        //advance
        currIndex += nextToken.length();
        return nextToken;
    }

    @Override
    public IToken peek() throws LexicalException {
        if (currIndex >= input.length) {
            // return EOF
        }
        //try tto consume character from input till there is atleast one transition in the FSA. If transition ends, output longest
        return null;
    }

    private void advance() {

    }
}
