package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.ASTNode;

public class Parser implements IParser {
    private final ILexer lexer;

    public Parser(ILexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public ASTNode parse() throws PLPException {
        return null;
    }
}
