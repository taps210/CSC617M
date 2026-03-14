package src.identifiers;

import org.junit.jupiter.api.Test;
import src.Scanner;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invalid identifier cases: 123abc (scanner recovers: INT_LIT + IDENT),
 * my-var (IDENT MINUS IDENT), my.var (IDENT DOT IDENT), @variable (throws).
 */
class InvalidIdentifiersTest extends ScannerTestBase {

    @Test
    void numberThenLetter_123abc_recoversAsIntThenIdent() {
        List<Token> tokens = tokenize("123abc");
        assertTrue(tokens.size() >= 2);
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(123, tokens.get(0).literal());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals("bc", tokens.get(1).lexeme());
    }

    @Test
    void numberThenLetter_12a_recoversAsIntThenEof() {
        List<Token> tokens = tokenize("12a");
        assertEquals(2, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(12, tokens.get(0).literal());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void floatThenLetter_3_14x_floatThenEof() {
        List<Token> tokens = tokenize("3.14x");
        assertEquals(2, tokens.size());
        assertEquals(TokenType.FLOAT_LIT, tokens.get(0).type());
        assertEquals("3.14", tokens.get(0).lexeme());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void numberThenUnderscore_0_ab_intIdentIdent() {
        List<Token> tokens = tokenize("0_ab");
        assertEquals(3, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals("0", tokens.get(0).lexeme());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals("ab", tokens.get(1).lexeme());
        assertEquals(TokenType.EOF, tokens.get(2).type());
    }

    @Test
    void recoveryMultipleLettersAfterNumber_12abc() {
        List<Token> tokens = tokenize("12abc");
        assertEquals(3, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(12, tokens.get(0).literal());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals("bc", tokens.get(1).lexeme());
        assertEquals(TokenType.EOF, tokens.get(2).type());
    }

    @Test
    void startsWithAt_throwsUnknownSymbol() {
        assertThrowsLexical(
                () -> new Scanner("@variable").tokenizeAll(),
                "Unknown symbol \"@\"", 1, 1);
    }

    @Test
    void hyphenInMiddle_myVar_threeTokensNotSingleIdent() {
        List<src.Token> tokens = tokenize("my-var");
        assertEquals(4, tokens.size());
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("my", tokens.get(0).lexeme());
        assertEquals(TokenType.MINUS, tokens.get(1).type());
        assertEquals(TokenType.IDENT, tokens.get(2).type());
        assertEquals("var", tokens.get(2).lexeme());
        assertEquals(TokenType.EOF, tokens.get(3).type());
    }

    @Test
    void dotInMiddle_myVar_threeTokensNotIdentifier() {
        List<Token> tokens = tokenize("my.var");
        assertEquals(4, tokens.size());
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("my", tokens.get(0).lexeme());
        assertEquals(TokenType.DOT, tokens.get(1).type());
        assertEquals(TokenType.IDENT, tokens.get(2).type());
        assertEquals("var", tokens.get(2).lexeme());
        assertEquals(TokenType.EOF, tokens.get(3).type());
    }
}
