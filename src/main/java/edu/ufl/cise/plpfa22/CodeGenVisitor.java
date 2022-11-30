package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.ast.Types.Type;

import java.util.Collections;
import java.util.List;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	final String packageName;
	final String className;
	final String sourceFileName;
	final String fullyQualifiedClassName;
	final String classDesc;
	private final SymbolTable<Integer> variableSymbolTable;
	private final SymbolTable<Integer> procedureSymbolTable;

	ClassWriter classWriter;
	int nextVariableFrameSlot = 1;
	int nextProcedureFrameSlot = 1;


	public CodeGenVisitor(String className, String packageName, String sourceFileName) {
		super();
		this.packageName = packageName;
		this.className = className;
		this.sourceFileName = sourceFileName;
		this.fullyQualifiedClassName = packageName + "/" + className;
		this.classDesc="L"+this.fullyQualifiedClassName+';';
		this.variableSymbolTable = new SymbolTable<>();
		this.procedureSymbolTable = new SymbolTable<>();
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException {
		MethodVisitor methodVisitor = (MethodVisitor)arg;
		methodVisitor.visitCode();

		final List<ConstDec> constDecs = block.constDecs;
		final List<VarDec> varDecs = block.varDecs;
		final List<ProcDec> procedureDecs = block.procedureDecs;

		variableSymbolTable.enterScope();
		for (final ConstDec dec : nullSafeList(constDecs)) {
			dec.visit(this, arg);
		}
		for (final VarDec dec : nullSafeList(varDecs)) {
			dec.visit(this, arg);
		}
		for (final ProcDec dec : nullSafeList(procedureDecs)) {
			final String name = dec.ident.getStringValue();
			procedureSymbolTable.insert(name, nextProcedureFrameSlot++);
		}
		for (final ProcDec dec : nullSafeList(procedureDecs)) {
			dec.visit(this, arg);
		}
		//add instructions from statement to method
		block.statement.visit(this, arg);
		methodVisitor.visitInsn(RETURN);
		methodVisitor.visitMaxs(0,0);
		methodVisitor.visitEnd();
		variableSymbolTable.exitScope();
		return null;
	}

	@Override
	public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
		final String name = constDec.ident.getStringValue();
		final MethodVisitor mv = (MethodVisitor) arg;
		//push on stack
		mv.visitLdcInsn(constDec.val);
		final Kind kind = constDec.ident.getKind();
		//pop from stack
		if (kind == Kind.NUM_LIT || kind == Kind.BOOLEAN_LIT) {
			mv.visitVarInsn(ISTORE, nextVariableFrameSlot);
		} else if (kind == Kind.STRING_LIT) {
			mv.visitVarInsn(ASTORE, nextVariableFrameSlot);
		} else {
			mv.visitVarInsn(FSTORE, nextVariableFrameSlot);
		}
		variableSymbolTable.insert(name, nextVariableFrameSlot++);
		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
		final String name = varDec.ident.getStringValue();
		final MethodVisitor mv = (MethodVisitor) arg;
		//push on stack
		mv.visitLdcInsn(null);
		final Kind kind = varDec.ident.getKind();
		//pop from stack
		if (kind == Kind.NUM_LIT || kind == Kind.BOOLEAN_LIT) {
			mv.visitVarInsn(ISTORE, nextVariableFrameSlot);
		} else if (kind == Kind.STRING_LIT) {
			mv.visitVarInsn(ASTORE, nextVariableFrameSlot);
		} else {
			mv.visitVarInsn(FSTORE, nextVariableFrameSlot);
		}
		variableSymbolTable.insert(name, nextVariableFrameSlot++);
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws PLPException {
		//create a classWriter and visit it
		classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		//Hint:  if you get failures in the visitMaxs, try creating a ClassWriter with 0
		// instead of ClassWriter.COMPUTE_FRAMES.  The result will not be a valid classfile,
		// but you will be able to print it so you can see the instructions.  After fixing,
		// restore ClassWriter.COMPUTE_FRAMES
		classWriter.visit(V16, ACC_PUBLIC | ACC_SUPER, fullyQualifiedClassName, null, "java/lang/Object", null);

		//get a method visitor for the main method.
		MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		//visit the block, passing it the methodVisitor
		program.block.visit(this, methodVisitor);
		//finish up the class
        classWriter.visitEnd();
        //return the bytes making up the classfile
		return classWriter.toByteArray();
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
        final MethodVisitor mv = (MethodVisitor)arg;
		//evaluate the expression
		statementAssign.expression.visit(this, arg);

		final String name = statementAssign.ident.getFirstToken().getStringValue();
		final int frameSlot = variableSymbolTable.find(name);

		final Kind kind = statementAssign.ident.getFirstToken().getKind();
		//update value in the frame
		if (kind == Kind.NUM_LIT || kind == Kind.BOOLEAN_LIT) {
			mv.visitVarInsn(ISTORE, frameSlot);
		} else if (kind == Kind.STRING_LIT) {
			mv.visitVarInsn(ASTORE, frameSlot);
		} else {
			mv.visitVarInsn(FSTORE, frameSlot);
		}
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
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
		final MethodVisitor mv = (MethodVisitor)arg;
		final IToken token = expressionIdent.getFirstToken();
		final String name = token.getStringValue();
		final int frameSlot = variableSymbolTable.find(name);
		final Kind kind = token.getKind();

		if (kind == Kind.NUM_LIT || kind == Kind.BOOLEAN_LIT) {
			mv.visitVarInsn(ILOAD, frameSlot);
		} else if (kind == Kind.STRING_LIT) {
			mv.visitVarInsn(ALOAD, frameSlot);
		} else {
			mv.visitVarInsn(FLOAD, frameSlot);
		}
		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		mv.visitLdcInsn(expressionNumLit.getFirstToken().getIntValue());
		return null;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		mv.visitLdcInsn(expressionStringLit.getFirstToken().getStringValue());
		return null;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		mv.visitLdcInsn(expressionBooleanLit.getFirstToken().getBooleanValue());
		return null;
	}

	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
		statementCall.visit(this, arg);
		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		statementOutput.expression.visit(this, arg);
		Type etype = statementOutput.expression.getType();
		String JVMType = (etype.equals(Type.NUMBER) ? "I" : (etype.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));
		String printlnSig = "(" + JVMType +")V";
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", printlnSig, false);
		return null;
	}

	@Override
	public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
		for (Statement s: statementBlock.statements) {
			s.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		statementIf.expression.visit(this, arg);
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
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		procDec.block.visit(this, arg);
		return null;
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
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
		nextVariableFrameSlot = 3;
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
