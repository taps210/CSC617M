package src.identifiers;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidIdentifiersTest extends ScannerTestBase {

    @Test
    void singleLetter_x() {
        List<Token> tokens = tokenize("x");
        assertTokenTypes(tokens, TokenType.IDENT, TokenType.EOF);
        assertEquals("x", tokens.get(0).lexeme());
    }

    @Test
    void singleLetter_a() {
        List<Token> tokens = tokenize("a");
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("a", tokens.get(0).lexeme());
    }

    @Test
    void myVar() {
        assertLexeme("myVar", "myVar");
    }

    @Test
    void underscorePrefix_temp() {
        assertLexeme("_temp", "_temp");
    }

    @Test
    void counter1() {
        assertLexeme("counter1", "counter1");
    }

    @Test
    void my_variable_name() {
        assertLexeme("my_variable_name", "my_variable_name");
    }

    @Test
    void __private() {
        assertLexeme("__private", "__private");
    }

    @Test
    void camelCase() {
        assertLexeme("CamelCase", "CamelCase");
    }

    @Test
    void snake_case() {
        assertLexeme("snake_case", "snake_case");
    }

    @Test
    void mixedCase_123() {
        assertLexeme("MixedCase_123", "MixedCase_123");
    }

    @Test
    void singleUnderscore() {
        assertTokenTypes(tokenize("_"), TokenType.IDENT, TokenType.EOF);
        assertEquals("_", tokenize("_").get(0).lexeme());
    }

    @Test
    void letterThenDigit_a1() {
        assertLexeme("a1", "a1");
    }

    @Test
    void underscoreThenDigits_123() {
        assertLexeme("_123", "_123");
    }

    @Test
    void multipleIdentifiers() {
        List<Token> tokens = tokenize("_start agent1 FooBar");
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("_start", tokens.get(0).lexeme());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals("agent1", tokens.get(1).lexeme());
        assertEquals(TokenType.IDENT, tokens.get(2).type());
        assertEquals("FooBar", tokens.get(2).lexeme());
        assertEquals(TokenType.EOF, tokens.get(3).type());
    }

    private void assertLexeme(String src, String expectedLexeme) {
        List<Token> tokens = tokenize(src);
        assertEquals(2, tokens.size());
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals(expectedLexeme, tokens.get(0).lexeme());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }
}
