/**
 * This code is provided for solely for use of students in the course COP5556 Programming Language Principles at the
 * University of Florida during the Fall Semester 2022 as part of the course project.  No other use is authorized.
 */

package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.IToken.Kind;

import static edu.ufl.cise.plpfa22.IToken.Kind.*;
import static edu.ufl.cise.plpfa22.Token.ESCAPED_SYMBOLS;

public class CompilerComponentFactory {

    public static ILexer getLexer(String input) {
        return new Lexer(input + "\0", getLanguageFSA());
    }

    public static IParser getParser(final ILexer lexer) {
        return new Parser(lexer);
    }

    private static FSA getLanguageFSA() {
        final FSANode start = new FSANode(false, null);
        start.addTransition(null, getReservedCharFSA(EOF, '\0'));
        start.addTransition(null, getReservedCharFSA(MINUS, '-'));
        start.addTransition(null, getReservedCharFSA(PLUS, '+'));
        start.addTransition(null, getReservedCharFSA(DOT, '.'));
        start.addTransition(null, getReservedCharFSA(COMMA, ','));
        start.addTransition(null, getReservedCharFSA(SEMI, ';'));
        // Not including the QUOTE token as it would result in identifying input "unterminated as a valid string with two tokens QUOTE and IDENT
        // start.addTransition(null, getReservedCharFSA(QUOTE, '"'));
        start.addTransition(null, getReservedCharFSA(LPAREN, '('));
        start.addTransition(null, getReservedCharFSA(RPAREN, ')'));
        start.addTransition(null, getReservedCharFSA(TIMES, '*'));
        start.addTransition(null, getReservedCharFSA(DIV, '/'));
        start.addTransition(null, getReservedCharFSA(MOD, '%'));
        start.addTransition(null, getReservedCharFSA(QUESTION, '?'));
        start.addTransition(null, getReservedCharFSA(BANG, '!'));
        start.addTransition(null, getReservedCharFSA(EQ, '='));
        start.addTransition(null, getReservedCharFSA(NEQ, '#'));
        start.addTransition(null, getReservedCharFSA(LT, '<'));
        start.addTransition(null, getReservedCharFSA(GT, '>'));
        start.addTransition(null, getReservedMultiCharFSA(ASSIGN, ":="));
        start.addTransition(null, getReservedMultiCharFSA(LE, "<="));
        start.addTransition(null, getReservedMultiCharFSA(GE, ">="));
        start.addTransition(null, getReservedMultiCharFSA(KW_CONST, "CONST"));
        start.addTransition(null, getReservedMultiCharFSA(KW_VAR, "VAR"));
        start.addTransition(null, getReservedMultiCharFSA(KW_PROCEDURE, "PROCEDURE"));
        start.addTransition(null, getReservedMultiCharFSA(KW_CALL, "CALL"));
        start.addTransition(null, getReservedMultiCharFSA(KW_BEGIN, "BEGIN"));
        start.addTransition(null, getReservedMultiCharFSA(KW_END, "END"));
        start.addTransition(null, getReservedMultiCharFSA(KW_IF, "IF"));
        start.addTransition(null, getReservedMultiCharFSA(KW_THEN, "THEN"));
        start.addTransition(null, getReservedMultiCharFSA(KW_WHILE, "WHILE"));
        start.addTransition(null, getReservedMultiCharFSA(KW_DO, "DO"));
        start.addTransition(null, getBooleanFSA());
        start.addTransition(null, getWhiteSpaceFSA());
        start.addTransition(null, getNewLineFSA("NEW_LINE"));
        start.addTransition(null, getIntFSA());
        start.addTransition(null, getIdentifierFSA());
        start.addTransition(null, getStringLiteralFSA());
        start.addTransition(null, getCommentFSA());

        return new FSA(start);
    }

    private static FSANode getReservedCharFSA(final String kind, final char ch) {
        final FSANode start = new FSANode(false, kind);
        start.addTransition(ch, new FSANode(true, kind));
        return start;
    }

    private static FSANode getReservedCharFSA(final Kind kind, final char ch) {
        final FSANode start = new FSANode(false, kind.name());
        start.addTransition(ch, new FSANode(true, kind.name()));
        return start;
    }

    private static FSANode getReservedMultiCharFSA(final Kind kind, final String str) {
        final FSANode start = new FSANode(false, kind.name());
        final char[] input = str.toCharArray();
        FSANode prevNode = start;
        for (char c : input) {
            final FSANode newNode = new FSANode(true, kind.name());
            prevNode.addTransition(c, newNode);
            prevNode = newNode;
        }
        return start;
    }

    private static FSANode getBooleanFSA() {
        final FSANode start = new FSANode(false, BOOLEAN_LIT.name());
        String[] booleans = {"TRUE", "FALSE"};
        for (String bool : booleans) {
            FSANode currNode = start;
            for (char c : bool.toCharArray()) {
                final FSANode newNode = new FSANode(true, BOOLEAN_LIT.name());
                currNode.addTransition(c, newNode);
                currNode = newNode;
            }
        }
        return start;
    }

    private static FSANode getIdentifierFSA() {
        final FSANode start = new FSANode(false, IDENT.name());

        final FSANode intsStart = new FSANode(true, IDENT.name());
        for (int i = 0; i <= 9; i++) {
            final FSANode node = new FSANode(true, IDENT.name());
            intsStart.addTransition(Character.forDigit(i, 10), node);
            node.addTransition(null, intsStart);
            node.addTransition(null, start);
        }

        for (char c = 'A'; c <= 'z'; c++) {
            if (c > 'Z' && c < 'a') continue;
            final FSANode newNode = new FSANode(true, IDENT.name());
            start.addTransition(c, newNode);
            newNode.addTransition(null, start);
            newNode.addTransition(null, intsStart);
        }

        final FSANode dollar = new FSANode(true, IDENT.name());
        dollar.addTransition(null, start);
        dollar.addTransition(null, intsStart);
        start.addTransition('$', dollar);

        final FSANode underscore = new FSANode(true, IDENT.name());
        underscore.addTransition(null, start);
        underscore.addTransition(null, intsStart);
        start.addTransition('_', underscore);

        return start;
    }

    private static FSANode getStringLiteralFSA() {
        final FSANode start = new FSANode(false, STRING_LIT.name());
        final FSANode openingQuotes = new FSANode(false, STRING_LIT.name());
        start.addTransition('"', openingQuotes);

        final FSANode closingQuotes = new FSANode(true, STRING_LIT.name());
        openingQuotes.addTransition('"', closingQuotes);

        //escape branch
        final FSANode escape = new FSANode(false, STRING_LIT.name());
        openingQuotes.addTransition(null, escape);

        final FSANode escapeSlash = new FSANode(false, STRING_LIT.name());
        escape.addTransition('\\', escapeSlash);

        for (char c : ESCAPED_SYMBOLS) {
            escapeSlash.addTransition(c, openingQuotes);
        }

        //other chars branch
        final FSANode otherNode = new FSANode(false, STRING_LIT.name());
        openingQuotes.addTransition(null, otherNode);

        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            final char ch = (char) i;
            if (ch == '\\' || ch == '"') {
                continue;
            }
            otherNode.addTransition(ch, openingQuotes);
        }
        return start;
    }

    private static FSANode getCommentFSA() {
        final FSANode start = new FSANode(false, "COMMENT");
        final FSANode commentStart = new FSANode(false, "COMMENT");
        start.addTransition('/', commentStart);

        final FSANode first = new FSANode(false, "COMMENT");
        commentStart.addTransition('/', first);
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            final char ch = (char) i;
            if (ch == '\n' || ch == '\r') {
                continue;
            }
            first.addTransition(ch, first);
        }
        first.addTransition(null, getNewLineFSA("COMMENT"));
        first.addTransition(null, getReservedCharFSA("COMMENT", '\0'));
        return start;
    }

    private static FSANode getWhiteSpaceFSA() {
        final FSANode start = new FSANode(false, "WHITE_SPACE");
        final FSANode nextNode = new FSANode(true, "WHITE_SPACE");

        start.addTransition(' ', nextNode);
        start.addTransition('\r', nextNode);
        start.addTransition('\t', nextNode);
        return start;
    }

    private static FSANode getNewLineFSA(String ofKind) {
        final FSANode start = new FSANode(false, ofKind);

        final FSANode one = new FSANode(true, ofKind);
        final FSANode two = new FSANode(false, ofKind);
        final FSANode three = new FSANode(true, ofKind);

        start.addTransition('\n', one);

        start.addTransition('\r', two);
        two.addTransition('\n', three);

        return start;
    }

    private static FSANode getIntFSA() {
        final FSANode start = new FSANode(false, NUM_LIT.name());
        final FSANode zero = new FSANode(true, NUM_LIT.name());
        start.addTransition('0', zero);

        final FSANode first = new FSANode(false, NUM_LIT.name());

        for (int i = 1; i <= 9; i++) {
            start.addTransition(Character.forDigit(i, 10), first);
        }

        final FSANode second = new FSANode(true, NUM_LIT.name());
        first.addTransition(null, second);

        for (int i = 0; i <= 9; i++) {
            second.addTransition(Character.forDigit(i, 10), second);
        }
        return start;
    }
}
