package src.literals;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntegersTest extends ScannerTestBase {

    @Test
    void zero() {
        List<Token> tokens = tokenize("0");
        assertTokenTypes(tokens, TokenType.INT_LIT, TokenType.EOF);
        assertEquals(0, tokens.get(0).literal());
    }

    @Test
    void integer_123() {
        List<Token> tokens = tokenize("123");
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(123, tokens.get(0).literal());
    }

    @Test
    void integer_42() {
        List<Token> tokens = tokenize("42");
        assertEquals(42, tokens.get(0).literal());
    }

    @Test
    void integer_999999() {
        List<Token> tokens = tokenize("999999");
        assertEquals(999999, tokens.get(0).literal());
    }

    @Test
    void leadingZeros_0001() {
        List<Token> tokens = tokenize("0001");
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(1, tokens.get(0).literal());
        assertEquals("0001", tokens.get(0).lexeme());
    }

    @Test
    void multipleIntegers() {
        List<Token> tokens = tokenize("0 42 999");
        assertTokenTypes(tokens, TokenType.INT_LIT, TokenType.INT_LIT, TokenType.INT_LIT, TokenType.EOF);
        assertEquals(0, tokens.get(0).literal());
        assertEquals(42, tokens.get(1).literal());
        assertEquals(999, tokens.get(2).literal());
    }
}
