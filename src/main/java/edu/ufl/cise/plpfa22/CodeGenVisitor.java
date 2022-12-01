package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;
import org.objectweb.asm.*;
import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.ast.Types.Type;

import java.lang.reflect.Array;
import java.util.*;

import static edu.ufl.cise.plpfa22.CodeGenUtils.toJMVClassName;
import static edu.ufl.cise.plpfa22.CodeGenUtils.toJVMClassDesc;
import static edu.ufl.cise.plpfa22.ast.Types.Type.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

    final String packageName;
    final String className;
    final String sourceFileName;
    final String fullyQualifiedClassName;
    final List<CodeGenUtils.GenClass> classes = new ArrayList<>();


    public CodeGenVisitor(String className, String packageName, String sourceFileName) {
        super();
        this.packageName = packageName;
        this.className = className;
        this.sourceFileName = sourceFileName;
        this.fullyQualifiedClassName = packageName + "/" + className;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLPException {
        //create a classWriter and visit it
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        //Hint:  if you get failures in the visitMaxs, try creating a ClassWriter with 0
        // instead of ClassWriter.COMPUTE_FRAMES.  The result will not be a valid classfile,
        // but you will be able to print it so you can see the instructions.  After fixing,
        // restore ClassWriter.COMPUTE_FRAMES
        classWriter.visit(V16, ACC_PUBLIC | ACC_SUPER, fullyQualifiedClassName, null, "java/lang/Object", new String[]{"java/lang/Runnable"});
        final MethodVisitor constructor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        final MethodVisitor mainVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mainVisitor.visitCode();
        mainVisitor.visitTypeInsn(NEW, fullyQualifiedClassName);
        mainVisitor.visitInsn(DUP);
        mainVisitor.visitMethodInsn(INVOKESPECIAL, fullyQualifiedClassName, "<init>", "()V", false);
        mainVisitor.visitMethodInsn(INVOKEVIRTUAL, fullyQualifiedClassName, "run", "()V", false);
        mainVisitor.visitInsn(RETURN);
        mainVisitor.visitMaxs(0, 0);
        mainVisitor.visitEnd();

        program.block.visit(this, Arrays.asList(classWriter, fullyQualifiedClassName, "this"));
        classWriter.visitEnd();
        classes.add(new CodeGenUtils.GenClass(fullyQualifiedClassName, classWriter.toByteArray()));
        return classes;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final ClassWriter blockWriter = (ClassWriter) args.get(0);
        final String className = (String) args.get(1);
        final String owner = (String) args.get(2);
        final MethodVisitor runVisitor = blockWriter.visitMethod(ACC_PUBLIC, "run", "()V", null, null);

        runVisitor.visitCode();

        final List<ConstDec> constDecs = block.constDecs;
        final List<VarDec> varDecs = block.varDecs;
        final List<ProcDec> procedureDecs = block.procedureDecs;

        for (final ConstDec dec : nullSafeList(constDecs)) {
            dec.visit(this, blockWriter);
        }

        for (final VarDec dec : nullSafeList(varDecs)) {
            dec.visit(this, blockWriter);
        }

        for (final ProcDec dec : nullSafeList(procedureDecs)) {
            dec.visit(this, Arrays.asList(owner, className, blockWriter));
        }

        block.statement.visit(this, Arrays.asList(runVisitor, className));
        runVisitor.visitInsn(RETURN);
        runVisitor.visitMaxs(0, 0);
        runVisitor.visitEnd();
        return null;
    }

    @Override
    public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
        final ClassWriter blockWriter = (ClassWriter) arg;
        final Type type = constDec.getType();
        blockWriter.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, constDec.ident.getStringValue(), type == NUMBER ? "I" : (type == BOOLEAN ? "Z" : "Ljava/lang/String;"), null, constDec.val).visitEnd();
        return null;
    }

    @Override
    public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
        final ClassWriter blockWriter = (ClassWriter) arg;
        final Type type = varDec.getType();
        blockWriter.visitField(ACC_PUBLIC, varDec.ident.getStringValue(), type == NUMBER ? "I" : (type == BOOLEAN ? "Z" : "Ljava/lang/String;"), null, null).visitEnd();
        return null;
    }

    @Override
    public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
        final ClassWriter procedureWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final List<Object> args = (List<Object>) arg;
        final String owner = (String) args.get(0);
        final String superClassName = (String) args.get(1);
        final ClassWriter currentClassWriter = (ClassWriter) args.get(2);
        final String procedureName = procDec.ident.getStringValue();

        final String className = superClassName + "$" + procedureName;
        procedureWriter.visit(V16, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", new String[]{"java/lang/Runnable"});


        currentClassWriter.visitNestMember(className);

        procedureWriter.visitNestHost(superClassName);
        procedureWriter.visitInnerClass(className, superClassName, procedureName, 0);

        procedureWriter.visitField(ACC_FINAL | ACC_SYNTHETIC, owner + "$0", toJVMClassDesc(superClassName), null, null).visitEnd();

        final MethodVisitor constructor = procedureWriter.visitMethod(0, "<init>", "(" + toJVMClassDesc(superClassName) + ")V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitVarInsn(ALOAD, 1);
        constructor.visitFieldInsn(PUTFIELD, className, owner + "$0", toJVMClassDesc(superClassName));
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        procDec.block.visit(this, Arrays.asList(procedureWriter, className, owner + "$this"));
        procedureWriter.visitEnd();
        classes.add(new CodeGenUtils.GenClass(className, procedureWriter.toByteArray()));
        return null;
    }

    @Override
    public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        final String className = (String) args.get(1);
        //evaluate the expression
        statementAssign.expression.visit(this, Arrays.asList(mv, className));

        final String name = statementAssign.ident.getFirstToken().getStringValue();
        final Type type = statementAssign.expression.getType();
        //update value
        mv.visitFieldInsn(PUTFIELD, className, name, type == NUMBER ? "I" : (type == BOOLEAN ? "Z" : "Ljava/lang/String;"));
        return null;
    }

    @Override
    public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
        final String name = statementCall.ident.getFirstToken().getStringValue();
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        final String className = (String) args.get(1);
        final String subClassName = className + "$" + name;
        mv.visitTypeInsn(NEW, subClassName);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, subClassName, "<init>", "(" + toJVMClassDesc(className) + ")V", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, subClassName, "run", "()V", false);
        return null;
    }

    @Override
    public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        final String className = (String) args.get(1);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        statementOutput.expression.visit(this, Arrays.asList(mv, className));
        Type etype = statementOutput.expression.getType();
        String JVMType = (etype.equals(NUMBER) ? "I" : (etype.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));
        String printlnSig = "(" + JVMType + ")V";
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", printlnSig, false);
        return null;
    }

    @Override
    public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
        for (Statement s : statementBlock.statements) {
            s.visit(this, arg);
        }
        return null;
    }

    @Override
    public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        final String className = (String) args.get(1);
        statementIf.expression.visit(this, Arrays.asList(mv, className));
        Label labelCompFalseBr = new Label();
        // if expression is false then jump to the end label
        mv.visitJumpInsn(IFEQ, labelCompFalseBr);
        // executed only if expression true
        statementIf.statement.visit(this, arg);
        mv.visitLabel(labelCompFalseBr);
        return null;
    }

    @Override
    public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        Type argType = expressionBinary.e0.getType();
        Kind op = expressionBinary.op.getKind();
        switch (argType) {
            case NUMBER -> {
                expressionBinary.e0.visit(this, arg);
                expressionBinary.e1.visit(this, arg);
                // all operations are defined for number
                switch (op) {
                    case PLUS -> mv.visitInsn(IADD);
                    case MINUS -> mv.visitInsn(ISUB);
                    case TIMES -> mv.visitInsn(IMUL);
                    case DIV -> mv.visitInsn(IDIV);
                    case MOD -> mv.visitInsn(IREM);
                    case EQ -> generateInstForComp(mv, IF_ICMPNE);
                    case NEQ -> generateInstForComp(mv, IF_ICMPEQ);
                    case LT -> generateInstForComp(mv, IF_ICMPGE);
                    case LE -> generateInstForComp(mv, IF_ICMPGT);
                    case GT -> generateInstForComp(mv, IF_ICMPLE);
                    case GE -> generateInstForComp(mv, IF_ICMPLT);
                    default -> throw new IllegalStateException("code gen bug in visitExpressionBinary NUMBER");
                }
            }
            case BOOLEAN -> {
                expressionBinary.e0.visit(this, arg);
                expressionBinary.e1.visit(this, arg);
                // MINUS, DIV, and MOD are not defined for BOOLEAN
                switch (op) {
                    case PLUS -> mv.visitInsn(IOR);
                    case TIMES -> mv.visitInsn(IAND);
                    case EQ -> generateInstForComp(mv, IF_ICMPNE);
                    case NEQ -> generateInstForComp(mv, IF_ICMPEQ);
                    case LT -> generateInstForComp(mv, IF_ICMPGE);
                    case LE -> generateInstForComp(mv, IF_ICMPGT);
                    case GT -> generateInstForComp(mv, IF_ICMPLE);
                    case GE -> generateInstForComp(mv, IF_ICMPLT);
                    default -> throw new IllegalStateException("code gen bug in visitExpressionBinary BOOLEAN");
                }
            }
            case STRING -> {
                expressionBinary.e0.visit(this, arg);
                expressionBinary.e1.visit(this, arg);
                // MINUS, DIV, MOD, and TIMES are not defined for STRING
                switch (op) {
                    case PLUS -> mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
                    case EQ -> mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                    case NEQ -> {
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                        generateInstForComp(mv, IFNE);
                    }
                    case LT -> generateInstForStringComp(mv, "startsWith", IAND);
                    case LE -> generateInstForStringComp(mv, "startsWith", IOR);
                    case GT -> generateInstForStringComp(mv, "endsWith", IAND);
                    case GE -> generateInstForStringComp(mv, "endsWith", IOR);
                    default -> throw new IllegalStateException("code gen bug in visitExpressionBinary STRING");
                }
            }
            default -> throw new IllegalStateException("code gen bug in visitExpressionBinary");
        }
        return null;
    }

    @Override
    public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        final String className = (String) args.get(1);

        final IToken token = expressionIdent.getFirstToken();
        final String name = token.getStringValue();
        final Type type = expressionIdent.getType();
        mv.visitFieldInsn(GETFIELD, className, name, type == NUMBER ? "I" : (type == BOOLEAN ? "Z" : "Ljava/lang/String;"));
        return null;
    }

    @Override
    public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        mv.visitLdcInsn(expressionNumLit.getFirstToken().getIntValue());
        return null;
    }

    @Override
    public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        mv.visitLdcInsn(expressionStringLit.getFirstToken().getStringValue());
        return null;
    }

    @Override
    public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
        final List<Object> args = (List<Object>) arg;
        final MethodVisitor mv = (MethodVisitor) args.get(0);
        mv.visitLdcInsn(expressionBooleanLit.getFirstToken().getBooleanValue());
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLPException {
        return null;
    }

    // have to give the opcode of the opposite compare instruction of the comparison that we want to do because the opposite compare instruction would be used to branch to a false condition
    // i.e. if evaluation of OPCODE instruction is true then sets false otherwise sets true
    public void generateInstForComp(MethodVisitor mv, int OPCODE) {
        Label labelCompFalseBr = new Label();
        mv.visitJumpInsn(OPCODE, labelCompFalseBr);
        // corresponds to boolean true
        mv.visitInsn(ICONST_1);
        Label labelPostComp = new Label();
        mv.visitJumpInsn(GOTO, labelPostComp);
        mv.visitLabel(labelCompFalseBr);
        // corresponds to boolean false
        mv.visitInsn(ICONST_0);
        mv.visitLabel(labelPostComp);
    }

    public void generateInstForStringComp(MethodVisitor mv, String methodName, int OPCODE) {
        // store the strings to variable array for later use in checking equality
        mv.visitVarInsn(ASTORE, 1);
        mv.visitVarInsn(ASTORE, 2);
        // load the strings to check for prefix
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 1);
        if (methodName.equals("startsWith")) {
            // swap the two strings so that the string on top is the one on which startsWith is called
            mv.visitInsn(SWAP);
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", methodName, "(Ljava/lang/String;)Z", false);
        // load the strings again to check for non equality
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        // if method being called for LE or GE then need to check for equality, not non-equality of the strings, so negate the result only in case of LT and GT
        if (OPCODE == IAND) {
            generateInstForComp(mv, IFNE);
        }
        // based on the requirement, AND or OR the two boolean results from not equals and startsWith/endsWith
        mv.visitInsn(OPCODE);
        // can reduce the number of operations by invoking not equals check only if startsWith/endsWith is true
    }

    private static <T> List<T> nullSafeList(final List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }
}
