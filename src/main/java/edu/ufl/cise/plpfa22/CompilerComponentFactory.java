/**
 * This code is provided for solely for use of students in the course COP5556 Programming Language Principles at the
 * University of Florida during the Fall Semester 2022 as part of the course project.  No other use is authorized.
 */

package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;

public class CompilerComponentFactory {

    public static ILexer getLexer(String input) {
        return new Lexer(input + "\0", getLanguageFSA());
    }

    private static FSA getLanguageFSA() {
		final FSANode start = new FSANode(false, null);
		start.addTransition(null, getReservedCharFSA(EOF, '\0'));
		start.addTransition(null, getReservedCharFSA(MINUS, '-'));
		start.addTransition(null, getReservedCharFSA(PLUS, '+'));
		start.addTransition(null, getWhiteSpaceFSA());
		start.addTransition(null, getNewLineFSA());
		start.addTransition(null, getIntFSA());
        return new FSA(start);
    }

    private static FSANode getReservedCharFSA(final Kind kind, final char ch) {
        final FSANode start = new FSANode(false, kind);
        start.addTransition(ch, new FSANode(true, kind));
        return start;
    }

	private static FSANode getWhiteSpaceFSA() {
		final FSANode start = new FSANode(false, WHITE_SPACE);

		final FSANode blankNode = new FSANode(true, WHITE_SPACE);
		final FSANode tabNode = new FSANode(true, WHITE_SPACE);
		final FSANode newLineNode = new FSANode(true, WHITE_SPACE);
		final FSANode carriageReturnNode = new FSANode(true, WHITE_SPACE);

		blankNode.addTransition(null, start);
		tabNode.addTransition(null, start);
		newLineNode.addTransition(null, start);
		carriageReturnNode.addTransition(null, start);

		start.addTransition(' ', blankNode);
		start.addTransition('\n', newLineNode);
		start.addTransition('\r', carriageReturnNode);
		start.addTransition('\t', tabNode);
		return start;
	}

	private static FSANode getNewLineFSA() {
		final FSANode start = new FSANode(false, NEW_LINE);

		final FSANode one = new FSANode(true, NEW_LINE);
		final FSANode two = new FSANode(true, NEW_LINE);

		start.addTransition('\n', one);
		one.addTransition('\r', two);

		one.addTransition(null, start);
		two.addTransition(null, start);

		return start;
	}

	private static FSANode getIntFSA() {
		final FSANode start = new FSANode(false, NUM_LIT);
		final FSANode zero = new FSANode(true, NUM_LIT);
		start.addTransition('0', zero);

		for (int i = 1; i <= 9; i++) {
			final FSANode node = new FSANode(true, NUM_LIT);
			start.addTransition(Character.forDigit(i, 10), node);
			node.addTransition(null, start);
		}
		return start;
	}
}
