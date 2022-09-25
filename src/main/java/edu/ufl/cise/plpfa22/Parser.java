package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;
import static edu.ufl.cise.plpfa22.IToken.Kind.*;
import edu.ufl.cise.plpfa22.ast.*;

import java.util.*;

import static edu.ufl.cise.plpfa22.IToken.Kind.NUM_LIT;

public class Parser implements IParser {
    private final ILexer lexer;
    private IToken t;

    private enum NON_TERMINALS {
        PROGRAM,
        BLOCK,
        STATEMENT,
        EXPRESSION,
        ADDITIVE_EXPRESSION,
        MULTIPLICATIVE_EXPRESSION,
        PRIMARY_EXPRESSION,
        CONST_VAL
    }

    private static final Map<NON_TERMINALS, Set<Kind>> FIRST_SETS = new HashMap<>();
    private static final Map<NON_TERMINALS, Set<Kind>> FOLLOW_SETS = new HashMap<>();

    public Parser(ILexer lexer) {
        this.lexer = lexer;
        for (NON_TERMINALS T: NON_TERMINALS.values()) {
            FIRST_SETS.put(T, getFirstSet(T));
            FOLLOW_SETS.put(T, getFollowSet(T));
        }
    }

    @Override
    public ASTNode parse() throws PLPException {
        return getProgramAST();
    }

    public Program getProgramAST() throws PLPException {
        Block b = getBlockAST();
        t = lexer.next();
        isKind(t, DOT);
        return new Program(b.firstToken, b);
    }

    public Block getBlockAST() throws PLPException {
        IToken firstToken = null;
        List<ConstDec> constDecs = new ArrayList<ConstDec>();
        while (lexer.peek().getKind() == KW_CONST) {
            t = lexer.next();
            if (firstToken == null) firstToken = t;
            t = lexer.next();
            isKind(t, IDENT);
            IToken ident = t;
            t = lexer.next();
            isKind(t, EQ);
            Expression constValExpression = getConstValAST();
            Object constVal = null;
            switch (constValExpression.firstToken.getKind()) {
                case STRING_LIT -> constVal = constValExpression.firstToken.getStringValue();
                case BOOLEAN_LIT -> constVal = constValExpression.firstToken.getBooleanValue();
                case NUM_LIT -> constVal = constValExpression.firstToken.getIntValue();
            }
            // TODO: pass calculated constVal
            // TODO: check if first token passed correctly
            constDecs.add(new ConstDec(ident, ident, constVal));
            // TODO: convert all the whiles like this to do while or something
            while (lexer.peek().getKind() == COMMA) {
                t = lexer.next();
                t = lexer.next();
                isKind(t, IDENT);
                ident = t;
                t = lexer.next();
                isKind(t, EQ);
                constValExpression = getConstValAST();
                switch (constValExpression.firstToken.getKind()) {
                    case STRING_LIT -> constVal = constValExpression.firstToken.getStringValue();
                    case BOOLEAN_LIT -> constVal = constValExpression.firstToken.getBooleanValue();
                    case NUM_LIT -> constVal = constValExpression.firstToken.getIntValue();
                }
                // TODO: check if first token passed correctly
                constDecs.add(new ConstDec(ident, ident, constVal));
            }
            t = lexer.next();
            isKind(t, SEMI);
        }

        List<VarDec> varDecs = new ArrayList<VarDec>();
        while (lexer.peek().getKind() == KW_VAR) {
            t = lexer.next();
            if (firstToken == null) firstToken = t;
            t = lexer.next();
            isKind(t, IDENT);
            // TODO: check if first token passed correctly
            varDecs.add(new VarDec(t, t));
            while (lexer.peek().getKind() == COMMA) {
                t = lexer.next();
                t = lexer.next();
                isKind(t, IDENT);
                // TODO: check if first token passed correctly
                varDecs.add(new VarDec(t, t));
            }
            t = lexer.next();
            isKind(t, SEMI);
        }

        List<ProcDec> procDecs = new ArrayList<ProcDec>();
        while (lexer.peek().getKind() == KW_PROCEDURE) {
            t = lexer.next();
            if (firstToken == null) firstToken = t;
            t = lexer.next();
            isKind(t, IDENT);
            IToken ident = t;
            t = lexer.next();
            isKind(t, SEMI);
            Block b = getBlockAST();
            t = lexer.next();
            isKind(t, SEMI);
            // TODO: check if first token passed correctly
            procDecs.add(new ProcDec(ident, ident, b));
        }

        Statement s = getStatementAST();
        if (firstToken == null) firstToken = s.firstToken;

        return new Block(firstToken, constDecs, varDecs, procDecs, s);
    }

    public Statement getStatementAST() throws PLPException {
        IToken nextToken = lexer.peek();
        IToken firstToken = nextToken;
        Kind kind = nextToken.getKind();
        Statement statement = null;
        switch (kind) {
            case IDENT -> {
                t = lexer.next();
                t = lexer.next();
                isKind(t, ASSIGN);
                statement = new StatementAssign(firstToken, new Ident(firstToken), getExpressionAST());
            }
            case KW_CALL -> {
                t = lexer.next();
                t = lexer.next();
                isKind(t, IDENT);
                statement = new StatementCall(firstToken, new Ident(t));
            }
            case QUESTION -> {
                t = lexer.next();
                t = lexer.next();
                isKind(t, IDENT);
                statement = new StatementInput(firstToken, new Ident(t));
            }
            case BANG -> {
                t = lexer.next();
                Expression expression = getExpressionAST();
                statement = new StatementOutput(firstToken, expression);
            }
            case KW_BEGIN -> {
                t = lexer.next();
                List<Statement> statements = new ArrayList<Statement>();
                statement = getStatementAST();
                statements.add(statement);
                while(lexer.peek().getKind() == SEMI) {
                    t = lexer.next();
                    statements.add(getStatementAST());
                }
                t = lexer.next();
                isKind(t, KW_END);
                statement = new StatementBlock(firstToken, statements);
            }
            case KW_IF -> {
                t = lexer.next();
                Expression expression = getExpressionAST();
                t = lexer.next();
                isKind(t, KW_THEN);
                statement = new StatementIf(firstToken, expression, getStatementAST());
            }
            case KW_WHILE -> {
                t = lexer.next();
                Expression expression = getExpressionAST();
                t = lexer.next();
                isKind(t, KW_DO);
                statement = new StatementWhile(firstToken, expression, getStatementAST());
            }
            default -> {
                if (FOLLOW_SETS.get(NON_TERMINALS.STATEMENT).contains(kind)) statement = new StatementEmpty(firstToken);
                else throw new SyntaxException();
            }
        }
        return statement;
    }

    public Expression getExpressionAST() throws PLPException {
        IToken firstToken = lexer.peek();
        Kind kind = firstToken.getKind();
        Expression expression = null;
        if (FIRST_SETS.get(NON_TERMINALS.EXPRESSION).contains(kind)) {
            expression = getAdditiveExpressionAST();
            Kind nextTokenKind = lexer.peek().getKind();
            while (nextTokenKind == LT || nextTokenKind == GT || nextTokenKind == EQ || nextTokenKind == NEQ || nextTokenKind == LE || nextTokenKind == GE) {
                t = lexer.next();
                expression = new ExpressionBinary(firstToken, expression, t, getAdditiveExpressionAST());
                nextTokenKind = lexer.peek().getKind();
            }
        } else throw new SyntaxException();
        return expression;
    }
    public Expression getAdditiveExpressionAST() throws PLPException {
        IToken firstToken = lexer.peek();
        Kind kind = firstToken.getKind();
        Expression expression = null;
        if (FIRST_SETS.get(NON_TERMINALS.MULTIPLICATIVE_EXPRESSION).contains(kind)) {
            expression = getMultiplicativeExpressionAST();
            Kind nextTokenKind = lexer.peek().getKind();
            while (nextTokenKind == PLUS || nextTokenKind == MINUS) {
                t = lexer.next();
                expression = new ExpressionBinary(firstToken, expression, t, getMultiplicativeExpressionAST());
                nextTokenKind = lexer.peek().getKind();
            }
        } else throw new SyntaxException();
        return expression;
    }

    public Expression getMultiplicativeExpressionAST() throws PLPException {
        IToken firstToken = lexer.peek();
        Kind kind = firstToken.getKind();
        Expression expression = null;
        if (FIRST_SETS.get(NON_TERMINALS.PRIMARY_EXPRESSION).contains(kind)) {
            expression = getPrimaryExpressionAST();
            Kind nextTokenKind = lexer.peek().getKind();
            while (nextTokenKind == TIMES || nextTokenKind == DIV || nextTokenKind == MOD) {
                t = lexer.next();
                expression = new ExpressionBinary(firstToken, expression, t, getPrimaryExpressionAST());
                nextTokenKind = lexer.peek().getKind();
            }
        } else throw new SyntaxException();
        return expression;
    }

    public Expression getPrimaryExpressionAST() throws PLPException {
        IToken firstToken = lexer.peek();
        Kind kind = firstToken.getKind();
        Expression expression = null;
        if (kind == IDENT) {
            t = lexer.next();
            expression = new ExpressionIdent(t);
        } else if (FIRST_SETS.get(NON_TERMINALS.CONST_VAL).contains(kind)) {
            expression = getConstValAST();
        } else if (kind == LPAREN) {
            t = lexer.next();
            expression = getExpressionAST();
            t = lexer.next();
            isKind(t, RPAREN);
        } else throw new SyntaxException();
        return expression;
    }

    public Expression getConstValAST() throws PLPException {
        t = lexer.next();
        Kind kind = t.getKind();
        Expression constVal = null;
        switch (kind) {
            case NUM_LIT -> constVal = new ExpressionNumLit(t);
            case STRING_LIT -> constVal = new ExpressionStringLit(t);
            case BOOLEAN_LIT -> constVal = new ExpressionBooleanLit(t);
            default -> throw new SyntaxException();
        }
        return constVal;
    }

    private void isKind(IToken t, Kind k) throws  PLPException {
        if (t.getKind() != k) throw new SyntaxException();
    }

    private Set<Kind> getFollowSet(NON_TERMINALS type) {
        Set<Kind> s = null;
        switch (type) {
            case PROGRAM -> s = new HashSet<Kind>();
            case BLOCK -> s = new HashSet<Kind>(List.of(DOT));
            case STATEMENT -> s = new HashSet<Kind>(List.of(DOT, SEMI, KW_END));
            case EXPRESSION -> {
                s = getFollowSet(NON_TERMINALS.STATEMENT);
                s.addAll(new HashSet<Kind>(List.of(KW_THEN, KW_DO, RPAREN)));
            }
            case ADDITIVE_EXPRESSION -> {
                s = getFollowSet(NON_TERMINALS.EXPRESSION);
                s.addAll(new HashSet<Kind>(List.of(LT, GT, EQ, NEQ, LE, GE)));
            }
            case MULTIPLICATIVE_EXPRESSION -> {
                s = getFollowSet(NON_TERMINALS.ADDITIVE_EXPRESSION);
                s.addAll(new HashSet<Kind>(List.of(PLUS, MINUS)));
            }
            case PRIMARY_EXPRESSION -> {
                s = getFollowSet(NON_TERMINALS.MULTIPLICATIVE_EXPRESSION);
                s.addAll(new HashSet<Kind>(List.of(TIMES, DIV, MOD)));
            }
            case CONST_VAL -> {
                s = getFollowSet(NON_TERMINALS.PRIMARY_EXPRESSION);
                s.addAll(new HashSet<Kind>(List.of(COMMA, SEMI)));
            }
        };
        return s;
    }

    private Set<Kind> getFirstSet(NON_TERMINALS type) {
        Set<Kind> s = null;
        switch (type) {
            case CONST_VAL -> s = new HashSet<Kind>(List.of(NUM_LIT, STRING_LIT, BOOLEAN_LIT));
            case PRIMARY_EXPRESSION -> s = new HashSet<Kind>(List.of(IDENT, NUM_LIT, STRING_LIT, BOOLEAN_LIT, LPAREN));
            case MULTIPLICATIVE_EXPRESSION -> s = getFirstSet(NON_TERMINALS.PRIMARY_EXPRESSION);
            case ADDITIVE_EXPRESSION -> s = getFirstSet(NON_TERMINALS.MULTIPLICATIVE_EXPRESSION);
            case EXPRESSION -> s = getFirstSet(NON_TERMINALS.ADDITIVE_EXPRESSION);
            case STATEMENT -> s = new HashSet<Kind>(List.of(IDENT, KW_CALL, QUESTION, BANG, KW_BEGIN, KW_IF, KW_WHILE));
            case BLOCK -> {
                s = getFirstSet(NON_TERMINALS.STATEMENT);
                s.addAll(new HashSet<Kind>(List.of(KW_CONST, KW_VAR, KW_PROCEDURE)));
            }
            case PROGRAM -> getFirstSet(NON_TERMINALS.BLOCK);
        }
        return s;
    }

}
