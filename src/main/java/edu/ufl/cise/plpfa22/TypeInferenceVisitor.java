package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.ast.*;
import edu.ufl.cise.plpfa22.ast.Types.Type;
import static edu.ufl.cise.plpfa22.ast.Types.Type.*;

public class TypeInferenceVisitor implements ASTVisitor {

    int totalChanges;

    public TypeInferenceVisitor() {
        totalChanges = 0;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLPException {
        boolean childrenTyped;
        do {
            childrenTyped = (boolean) visitBlock(program.block, arg);
            if (!childrenTyped && totalChanges == 0) throw new TypeCheckException();
            totalChanges = 0;
        } while (!childrenTyped);
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLPException {
        boolean childrenTyped = true;
        for (final ConstDec dec : block.constDecs) {
            childrenTyped &= (boolean) visitConstDec(dec, arg);
        }
        for (final ProcDec dec: block.procedureDecs) {
            childrenTyped &= (boolean) visitProcedure(dec, arg);
        }
        childrenTyped &= (boolean) visitStatement(block.statement, arg);
        return childrenTyped;
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
        } else if (statement instanceof StatementCall) {
            return visitStatementCall((StatementCall) statement, arg);
        } else {
            return visitStatementEmpty((StatementEmpty) statement, arg);
        }
    }

    @Override
    public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
        boolean expressionTypeChecked = (boolean) visitExpression(statementAssign.expression, arg);
        if (statementAssign.ident.getDec() instanceof ConstDec) throw new TypeCheckException();
        Type expressionType = statementAssign.expression.getType();
        Type identType = statementAssign.ident.getDec().getType();
        if (expressionType != null && identType == null) {
            setType(statementAssign.ident.getDec(), expressionType);
        } else if (expressionType == null && identType != null) {
            setType(statementAssign.expression, identType);
        }
        if (expressionType != null && identType != null && (expressionType != identType || (expressionType == PROCEDURE))) throw new TypeCheckException();
        return statementAssign.expression.getType() != null & expressionTypeChecked;
    }

    @Override
    public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
        Type type = statementCall.ident.getDec().getType();
        if (type != null && type != PROCEDURE) throw new TypeCheckException();
        return type != null;
    }

    @Override
    public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
        Type type = statementInput.ident.getDec().getType();
        if (statementInput.ident.getDec() instanceof ConstDec) throw new TypeCheckException();
        if (type != null && type != STRING && type != NUMBER && type != BOOLEAN) throw new TypeCheckException();
        return type != null;
    }

    @Override
    public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
        boolean expressionTyped = (boolean) visitExpression(statementOutput.expression, arg);
        Type type = statementOutput.expression.getType();
        if (type != null && type != STRING && type != NUMBER && type != BOOLEAN) throw new TypeCheckException();
        return expressionTyped & type != null;
    }

    @Override
    public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
        boolean childrenTyped = true;
        for (final Statement statement : statementBlock.statements) {
            childrenTyped &= (boolean) visitStatement(statement, arg);
        }
        return childrenTyped;
    }

    @Override
    public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
        boolean childrenTyped = (boolean) visitExpression(statementIf.expression, arg) & (boolean) visitStatement(statementIf.statement, arg);
        Type expressionType = statementIf.expression.getType();
        if (expressionType != null && expressionType != BOOLEAN) throw new TypeCheckException();
        return childrenTyped;
    }

    @Override
    public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
        boolean childrenTyped = (boolean) visitExpression(statementWhile.expression, arg) & (boolean) visitStatement(statementWhile.statement, arg);
        Type expressionType = statementWhile.expression.getType();
        if (expressionType != null && expressionType != BOOLEAN) throw new TypeCheckException();
        return childrenTyped;
    }

    @Override
    public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
        Kind opKind = expressionBinary.op.getKind();
        Type expressionType = expressionBinary.getType();
        if (expressionType != null && (opKind != Kind.EQ && opKind != Kind.NEQ && opKind != Kind.LT && opKind != Kind.LE && opKind != Kind.GT && opKind != Kind.GE)) {
            setType(expressionBinary.e0, expressionType);
            setType(expressionBinary.e1, expressionType);
        }
        boolean childrenTyped = (boolean) visitExpression(expressionBinary.e0, arg) & (boolean) visitExpression(expressionBinary.e1, arg);
        Type leftType = expressionBinary.e0.getType();
        Type rightType = expressionBinary.e1.getType();
        if (leftType == null && rightType != null) {
            setType(expressionBinary.e0, rightType);
            leftType = rightType;
        } else if (leftType != null && rightType == null) {
            setType(expressionBinary.e1, leftType);
            rightType = leftType;
        }
        // in subsequent conditions we don't have to null check for both left and right, just one is enough since by this time if any one of them is non-null, other one is guaranteed to be non-null
        if (leftType != null && leftType != rightType) throw new TypeCheckException();
        if (leftType != null) {
            if (opKind == Kind.PLUS) {
                if (leftType == PROCEDURE) throw new TypeCheckException();
                else setType(expressionBinary, leftType);
            } else if (opKind == Kind.MINUS || opKind == Kind.DIV || opKind == Kind.MOD) {
                if (leftType != NUMBER) throw new TypeCheckException();
                else setType(expressionBinary, NUMBER);
            } else if (opKind == Kind.TIMES) {
                if (leftType != NUMBER && leftType != BOOLEAN) throw new TypeCheckException();
                else setType(expressionBinary, leftType);
            } else {
                if (leftType == PROCEDURE) throw new TypeCheckException();
                else setType(expressionBinary, BOOLEAN);
            }
        }
        return childrenTyped & expressionBinary.getType() != null;
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
        } else {
            return visitExpressionBinary((ExpressionBinary) expression, arg);
        }
    }

    @Override
    public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
        Type expressionIdentType = expressionIdent.getType();
        if (expressionIdentType != null) {
            setType(expressionIdent.getDec(), expressionIdentType);
        } else {
            setType(expressionIdent, expressionIdent.getDec().getType());
        }
        Type type = expressionIdent.getType();
        return type != null;
    }

    @Override
    public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
        Type expressionNumLitType = expressionNumLit.getType();
        if (expressionNumLitType != null && expressionNumLitType != NUMBER) throw new TypeCheckException();
        setType(expressionNumLit, NUMBER);
        return true;
    }

    @Override
    public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
        Type expressionStringLitType = expressionStringLit.getType();
        if (expressionStringLitType != null && expressionStringLitType != STRING) throw new TypeCheckException();
        setType(expressionStringLit, STRING);
        return true;
    }

    @Override
    public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
        Type expressionBooleanLitType = expressionBooleanLit.getType();
        if (expressionBooleanLitType != null && expressionBooleanLitType != BOOLEAN) throw new TypeCheckException();
        setType(expressionBooleanLit, BOOLEAN);
        return true;
    }

    @Override
    public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
        if (constDec.val instanceof String) {
            setType(constDec, STRING);
        } else if (constDec.val instanceof Number) {
            setType(constDec, NUMBER);
        } else {
            setType(constDec, BOOLEAN);
        }
        return true;
    }

    @Override
    public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
        return true;
    }

    @Override
    public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
        setType(procDec, PROCEDURE);
        return visitBlock(procDec.block, arg);
    }

    @Override
    public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
        return true;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLPException {
        return true;
    }

    public void setType(Declaration d, Type t) throws TypeCheckException {
        if (t == null) return;
        if (d.getType() == null) {
            d.setType(t);
            totalChanges++;
        } else {
            if (t != d.getType()) throw new TypeCheckException();
        }
    }
    public void setType(Expression e, Type t) throws TypeCheckException{
        if (t == null) return;
        if (e.getType() == null) {
            e.setType(t);
            totalChanges++;
        } else {
            if (t != e.getType()) throw new TypeCheckException();
        }
    }
}
