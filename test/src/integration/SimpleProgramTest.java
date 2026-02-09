package src.integration;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleProgramTest extends ScannerTestBase {

    @Test
    void voidMainPrintHi() {
        String src = "void main() { print(\"hi\"); }";
        List<Token> tokens = tokenize(src);
        assertTrue(tokens.size() >= 2);
        assertEquals(TokenType.VOID, tokens.get(0).type());
        assertEquals(TokenType.MAIN, tokens.get(1).type());
        assertEquals(TokenType.LPAREN, tokens.get(2).type());
        assertEquals(TokenType.RPAREN, tokens.get(3).type());
        assertEquals(TokenType.LBRACE, tokens.get(4).type());
        assertEquals(TokenType.PRINT, tokens.get(5).type());
        assertEquals(TokenType.LPAREN, tokens.get(6).type());
        assertEquals(TokenType.STRING_LIT, tokens.get(7).type());
        assertEquals("hi", tokens.get(7).literal());
        assertEquals(TokenType.RPAREN, tokens.get(8).type());
        assertEquals(TokenType.SEMI, tokens.get(9).type());
        assertEquals(TokenType.RBRACE, tokens.get(10).type());
        assertEquals(TokenType.EOF, tokens.get(11).type());
    }

    @Test
    void simpleProgram_withIntAndPrint() {
        String src = "void main() { int x; x = 5; print(x); }";
        List<Token> tokens = tokenize(src);
        assertEquals(TokenType.VOID, tokens.get(0).type());
        assertEquals(TokenType.MAIN, tokens.get(1).type());
        assertEquals(TokenType.LPAREN, tokens.get(2).type());
        assertEquals(TokenType.RPAREN, tokens.get(3).type());
        assertEquals(TokenType.LBRACE, tokens.get(4).type());
        assertEquals(TokenType.INT, tokens.get(5).type());
        assertEquals(TokenType.IDENT, tokens.get(6).type());
        assertEquals("x", tokens.get(6).lexeme());
        assertEquals(TokenType.SEMI, tokens.get(7).type());
        assertEquals(TokenType.IDENT, tokens.get(8).type());
        assertEquals(TokenType.ASSIGN, tokens.get(9).type());
        assertEquals(TokenType.INT_LIT, tokens.get(10).type());
        assertEquals(5, tokens.get(10).literal());
        assertEquals(TokenType.SEMI, tokens.get(11).type());
        assertEquals(TokenType.PRINT, tokens.get(12).type());
        assertEquals(TokenType.LPAREN, tokens.get(13).type());
        assertEquals(TokenType.IDENT, tokens.get(14).type());
        assertEquals(TokenType.RPAREN, tokens.get(15).type());
        assertEquals(TokenType.SEMI, tokens.get(16).type());
        assertEquals(TokenType.RBRACE, tokens.get(17).type());
        assertEquals(TokenType.EOF, tokens.get(18).type());
    }
}
