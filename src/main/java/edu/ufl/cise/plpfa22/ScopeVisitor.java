package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;

import java.util.Collections;
import java.util.List;

public class ScopeVisitor implements ASTVisitor {
    private final SymbolTable symbolTable;
    private int nestLevel;

    public ScopeVisitor() {
        this.symbolTable = new SymbolTable();
        this.nestLevel = -1;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLPException {
        return visitBlock(program.block, arg);
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLPException {
        final List<ConstDec> constDecs = block.constDecs;
        final List<VarDec> varDecs = block.varDecs;
        final List<ProcDec> procedureDecs = block.procedureDecs;

        symbolTable.enterScope();
        nestLevel++;
        for (final ConstDec dec : nullSafeList(constDecs)) {
            visitConstDec(dec, arg);
        }
        for (final VarDec dec : nullSafeList(varDecs)) {
            visitVarDec(dec, arg);
        }
        for (final ProcDec dec : nullSafeList(procedureDecs)) {
            final String name = dec.ident.getStringValue();
            final int insertNestLevel = symbolTable.insert(name, dec);
            if (insertNestLevel == -1) {
                throw new ScopeException();
            }
            dec.setNest(insertNestLevel);
        }
        for (final ProcDec dec : nullSafeList(procedureDecs)) {
            visitProcedure(dec, arg);
        }
        visitStatement(block.statement, arg);
        nestLevel--;
        symbolTable.exitScope();
        return null;
    }

    private Object visitStatement(final Statement statement, final Object arg) throws PLPException {
        if (statement instanceof StatementAssign) {
            return visitStatementAssign((StatementAssign) statement, arg);
        } else if (statement instanceof StatementBlock) {
            return visitStatementBlock((StatementBlock) statement, arg);
        } else if (statement instanceof StatementOutput) {
            return visitStatementOutput((StatementOutput) statement, arg);
        } else if (statement instanceof StatementInput) {
            return visitStatementInput((StatementInput) statement, arg);
        } else if (statement instanceof StatementWhile) {
            return visitStatementWhile((StatementWhile) statement, arg);
        } else if (statement instanceof StatementIf) {
            return visitStatementIf((StatementIf) statement, arg);
        } else if (statement instanceof StatementEmpty) {
            return visitStatementEmpty((StatementEmpty) statement, arg);
        } else if (statement instanceof StatementCall) {
            return visitStatementCall((StatementCall) statement, arg);
        }
        throw new ScopeException();
    }

    @Override
    public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
        visitIdent(statementAssign.ident, arg);
        visitExpression(statementAssign.expression, arg);
        return null;
    }

    @Override
    public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
        return visitIdent(statementCall.ident, arg);
    }

    @Override
    public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
        return visitIdent(statementInput.ident, arg);
    }

    @Override
    public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
        return visitExpression(statementOutput.expression, arg);
    }

    @Override
    public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
        for (final Statement statement : statementBlock.statements) {
            visitStatement(statement, arg);
        }
        return null;
    }

    @Override
    public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
        visitExpression(statementIf.expression, arg);
        visitStatement(statementIf.statement, arg);
        return null;
    }

    @Override
    public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
        visitExpression(statementWhile.expression, arg);
        visitStatement(statementWhile.statement, arg);
        return null;
    }

    @Override
    public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
        visitExpression(expressionBinary.e0, arg);
        visitExpression(expressionBinary.e1, arg);
        return null;
    }

    private Object visitExpression(final Expression expression, final Object arg) throws PLPException {
        if (expression instanceof ExpressionIdent) {
            return visitExpressionIdent((ExpressionIdent) expression, arg);
        } else if (expression instanceof ExpressionNumLit) {
            return visitExpressionNumLit((ExpressionNumLit) expression, arg);
        } else if (expression instanceof ExpressionStringLit) {
            return visitExpressionStringLit((ExpressionStringLit) expression, arg);
        } else if (expression instanceof ExpressionBooleanLit) {
            return visitExpressionBooleanLit((ExpressionBooleanLit) expression, arg);
        } else if (expression instanceof ExpressionBinary) {
            return visitExpressionBinary((ExpressionBinary) expression, arg);
        }
        throw new ScopeException();
    }

    @Override
    public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
        final String name = expressionIdent.getFirstToken().getStringValue();
        final Declaration dec = symbolTable.find(name);
        if (dec == null) {
            throw new ScopeException();
        }
        expressionIdent.setDec(dec);
        expressionIdent.setNest(nestLevel);
        return null;
    }

    @Override
    public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
        //no-op
        return null;
    }

    @Override
    public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
        //no-op
        return null;
    }

    @Override
    public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
        //no-op
        return null;
    }

    @Override
    public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
        final String name = constDec.ident.getStringValue();
        final int insertNestLevel = symbolTable.insert(name, constDec);
        if (insertNestLevel == -1) {
            throw new ScopeException();
        }
        constDec.setNest(insertNestLevel);
        return null;
    }

    @Override
    public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
        final String name = varDec.ident.getStringValue();
        final int insertNestLevel = symbolTable.insert(name, varDec);
        if (insertNestLevel == -1) {
            throw new ScopeException();
        }
        varDec.setNest(insertNestLevel);
        return null;
    }

    @Override
    public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
        visitBlock(procDec.block, arg);
        return null;
    }

    @Override
    public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
        //no-op
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLPException {
        final String name = ident.getFirstToken().getStringValue();
        final Declaration dec = symbolTable.find(name);
        if (dec == null) {
            throw new ScopeException();
        }
        ident.setDec(dec);
        ident.setNest(nestLevel);
        return null;
    }

    private static <T> List<T> nullSafeList(final List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }
}
