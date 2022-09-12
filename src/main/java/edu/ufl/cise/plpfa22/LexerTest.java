/**
 * This code is provided for solely for use of students in the course COP5556 Programming Language Principles at the
 * University of Florida during the Fall Semester 2022 as part of the course project.  No other use is authorized.
 */

package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;
import org.junit.jupiter.api.Test;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LexerTest {

    /*** Useful functions ***/
    ILexer getLexer(String input) {
        return CompilerComponentFactory.getLexer(input);
    }

    //makes it easy to turn output on and off (and less typing than System.out.println)
    static final boolean VERBOSE = true;

    void show(Object obj) {
        if (VERBOSE) {
            System.out.println(obj);
        }
    }

    //check that this token has the expected kind
    void checkToken(IToken t, Kind expectedKind) {
        assertEquals(expectedKind, t.getKind());
    }

    //check that the token has the expected kind and position
    void checkToken(IToken t, Kind expectedKind, int expectedLine, int expectedColumn) {
        assertEquals(expectedKind, t.getKind());
        assertEquals(new IToken.SourceLocation(expectedLine, expectedColumn), t.getSourceLocation());
    }

    //check that this token is an IDENT and has the expected name
    void checkIdent(IToken t, String expectedName) {
        assertEquals(Kind.IDENT, t.getKind());
        assertEquals(expectedName, String.valueOf(t.getText()));
    }

    //check that this token is an IDENT, has the expected name, and has the expected position
    void checkIdent(IToken t, String expectedName, int expectedLine, int expectedColumn) {
        checkIdent(t, expectedName);
        assertEquals(new IToken.SourceLocation(expectedLine, expectedColumn), t.getSourceLocation());
    }


    //check that this token is an NUM_LIT with expected int value
    void checkInt(IToken t, int expectedValue) {
        assertEquals(Kind.NUM_LIT, t.getKind());
        assertEquals(expectedValue, t.getIntValue());
    }

    //check that this token  is an NUM_LIT with expected int value and position
    void checkInt(IToken t, int expectedValue, int expectedLine, int expectedColumn) {
        checkInt(t, expectedValue);
        assertEquals(new IToken.SourceLocation(expectedLine, expectedColumn), t.getSourceLocation());
    }

    //check that this token is the EOF token
    void checkEOF(IToken t) {
        checkToken(t, Kind.EOF);
    }

    /***Tests****/

    //The lexer should add an EOF token to the end.
    @Test
    void testEmpty() throws LexicalException {
        String input = "";
        show(input);
        ILexer lexer = getLexer(input);
        show(lexer);
        checkEOF(lexer.next());
    }

    //A couple of single character tokens
    @Test
    void testSingleChar0() throws LexicalException {
        String input = """
                + 
                - 	 
                """;
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), Kind.PLUS, 1, 1);
        checkToken(lexer.next(), Kind.MINUS, 2, 1);
        checkEOF(lexer.next());
    }

    //comments should be skipped
    @Test
    void testComment0() throws LexicalException {
        //Note that the quotes around "This is a string" are passed to the lexer.
        String input = """
                "This is a string"
                // this is a comment
                *
                """;
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), Kind.STRING_LIT, 1, 1);
        checkToken(lexer.next(), Kind.TIMES, 3, 1);
        checkEOF(lexer.next());
    }

    //Example for testing input with an illegal character
    @Test
    void testError0() throws LexicalException {
        String input = """
                abc
                @
                """;
        show(input);
        ILexer lexer = getLexer(input);
        //this check should succeed
        checkIdent(lexer.next(), "abc", 1, 1);
        //this is expected to throw an exception since @ is not a legal
        //character unless it is part of a string or comment
        assertThrows(LexicalException.class, () -> {
            @SuppressWarnings("unused")
            IToken token = lexer.next();
        });
    }

    //Several identifiers to test positions
    @Test
    public void testIdent0() throws LexicalException {
        String input = """
                abc
                  def
                     ghi

                """;
        show(input);
        ILexer lexer = getLexer(input);
        checkIdent(lexer.next(), "abc", 1, 1);
        checkIdent(lexer.next(), "def", 2, 3);
        checkIdent(lexer.next(), "ghi", 3, 6);
        checkEOF(lexer.next());
    }


    @Test
    public void testIdenInt() throws LexicalException {
        String input = """
                a123 456b
                """;
        show(input);
        ILexer lexer = getLexer(input);
        checkIdent(lexer.next(), "a123", 1, 1);
        checkInt(lexer.next(), 456, 1, 6);
        checkIdent(lexer.next(), "b", 1, 9);
        checkEOF(lexer.next());
    }


    //Example showing how to handle number that are too big.
    @Test
    public void testIntTooBig() throws LexicalException {
        String input = """
                42
                99999999999999999999999999999999999999999999999999999999999999999999999
                """;
        final ILexer lexer = getLexer(input);
        checkInt(lexer.next(), 42, 1, 1);
        assertThrows(LexicalException.class, lexer::next);
    }

    @Test
    public void testInt() throws LexicalException {
        String input = """
                42
                0
                10
                135
                200
                257
                3000
                379
                40000
                426
                500000
                538
                60
                649
                700
                757
                800
                88
                90
                913
                00
                01
                """;
        final ILexer lexer = getLexer(input);
        checkInt(lexer.next(), 42, 1, 1);
        checkInt(lexer.next(), 0, 2, 1);
        checkInt(lexer.next(), 10, 3, 1);
        checkInt(lexer.next(), 135, 4, 1);
        checkInt(lexer.next(), 200, 5, 1);
        checkInt(lexer.next(), 257, 6, 1);
        checkInt(lexer.next(), 3000, 7, 1);
        checkInt(lexer.next(), 379, 8, 1);
        checkInt(lexer.next(), 40000, 9, 1);
        checkInt(lexer.next(), 426, 10, 1);
        checkInt(lexer.next(), 500000, 11, 1);
        checkInt(lexer.next(), 538, 12, 1);
        checkInt(lexer.next(), 60, 13, 1);
        checkInt(lexer.next(), 649, 14, 1);
        checkInt(lexer.next(), 700, 15, 1);
        checkInt(lexer.next(), 757, 16, 1);
        checkInt(lexer.next(), 800, 17, 1);
        checkInt(lexer.next(), 88, 18, 1);
        checkInt(lexer.next(), 90, 19, 1);
        checkInt(lexer.next(), 913, 20, 1);
        checkInt(lexer.next(), 0, 21, 1);
        checkInt(lexer.next(), 0, 21, 2);
        checkInt(lexer.next(), 0, 22, 1);
        checkInt(lexer.next(), 1, 22, 2);
        checkEOF(lexer.next());
    }

    @Test
    public void testWhiteSpace() throws LexicalException {
        String input = "123 \n\r \t\t\r456";
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), NUM_LIT, 1, 1);
        checkToken(lexer.next(), NUM_LIT, 2, 6);
        checkEOF(lexer.next());
    }

    @Test
    public void testNewLine() throws LexicalException {
        String input = "123 \n\r \t\t\r\r\n\n\n 456";
        show(input);
        ILexer lexer = getLexer(input);
        checkToken(lexer.next(), NUM_LIT, 1, 1);
        checkToken(lexer.next(), NUM_LIT, 5, 2);
        checkEOF(lexer.next());
    }

    @Test
    public void testEscapeSequences0() throws LexicalException {
        String input = "\"\\b \\t \\n \\f \\r \"";
        show(input);
        ILexer lexer = getLexer(input);
        IToken t = lexer.next();
        String val = t.getStringValue();
        String expectedStringValue = "\b \t \n \f \r ";
        assertEquals(expectedStringValue, val);
        String text = String.valueOf(t.getText());
        String expectedText = "\"\\b \\t \\n \\f \\r \"";
        assertEquals(expectedText, text);
    }

    @Test
    public void testEscapeSequences1() throws LexicalException {
        String input = "   \" ...  \\\"  \\\'  \\\\  \"";
        show(input);
        ILexer lexer = getLexer(input);
        IToken t = lexer.next();
        String val = t.getStringValue();
        String expectedStringValue = " ...  \"  \'  \\  ";
        assertEquals(expectedStringValue, val);
        String text = String.valueOf(t.getText());
        String expectedText = "\" ...  \\\"  \\\'  \\\\  \""; //almost the same as input, but white space is omitted
        assertEquals(expectedText, text);
    }
}

	//A couple of boolean tokens
	@Test
	void testBooleans() throws LexicalException {
		String input = """
				TRUE
				FALSE
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.BOOLEAN_LIT, 1,1);
		checkToken(lexer.next(), Kind.BOOLEAN_LIT, 2,1);
		checkEOF(lexer.next());
	}
}
