/**  This code is provided for solely for use of students in the course COP5556 Programming Language Principles at the 
 * University of Florida during the Fall Semester 2022 as part of the course project.  No other use is authorized. 
 */

package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;

public class CompilerComponentFactory {

	public static ILexer getLexer(String input) {
		return new Lexer(input + "\0", getLanguageFSA());
	}

	private static FSA getLanguageFSA() {
		final FSANode start = new FSANode(false, null);
		start.addTransition('\0', new FSANode(true, EOF));
		start.addTransition('+', new FSANode(true, EOF));
		start.addTransition('-', new FSANode(true, EOF));
		return new FSA(start);
	}
}
