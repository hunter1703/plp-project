package edu.ufl.cise.plpfa22;

public class Lexer implements ILexer {
    private final char[] input;
    private final FSA fsa;

    public Lexer(String input, FSA fsa) {
        this.input = input == null ? new char[0] : input.toCharArray();
        this.fsa = fsa;
    }

    @Override
    public IToken next() throws LexicalException {
        return null;
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }

}
