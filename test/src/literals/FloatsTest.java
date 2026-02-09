package src.literals;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FloatsTest extends ScannerTestBase {

    @Test
    void zeroPointZero() {
        List<Token> tokens = tokenize("0.0");
        assertTokenTypes(tokens, TokenType.FLOAT_LIT, TokenType.EOF);
        assertEquals(0.0, (Double) tokens.get(0).literal(), 1e-9);
    }

    @Test
    void threePointFourteen() {
        List<Token> tokens = tokenize("3.14");
        assertEquals(TokenType.FLOAT_LIT, tokens.get(0).type());
        assertEquals(3.14, (Double) tokens.get(0).literal(), 1e-9);
    }

    @Test
    void float_123Point456() {
        List<Token> tokens = tokenize("123.456");
        assertEquals(123.456, (Double) tokens.get(0).literal(), 1e-9);
    }

    @Test
    void zeroPointFive() {
        List<Token> tokens = tokenize("0.5");
        assertEquals(0.5, (Double) tokens.get(0).literal(), 1e-9);
    }

    @Test
    void leadingDot_pointFive() {
        List<Token> tokens = tokenize(".5");
        assertEquals(TokenType.FLOAT_LIT, tokens.get(0).type());
        assertEquals(0.5, (Double) tokens.get(0).literal(), 1e-9);
    }

    @Test
    void trailingDot_5Point() {
        List<Token> tokens = tokenize("5.");
        assertEquals(TokenType.FLOAT_LIT, tokens.get(0).type());
        assertEquals(5.0, (Double) tokens.get(0).literal(), 1e-9);
    }

    @Test
    void threePoint_trailingDot() {
        List<Token> tokens = tokenize("3.");
        assertEquals(TokenType.FLOAT_LIT, tokens.get(0).type());
        assertEquals(3.0, (Double) tokens.get(0).literal(), 1e-9);
    }

    @Test
    void multipleFloats() {
        List<Token> tokens = tokenize("0.0 .5 3.14 3.");
        assertTokenTypes(tokens, TokenType.FLOAT_LIT, TokenType.FLOAT_LIT, TokenType.FLOAT_LIT, TokenType.FLOAT_LIT, TokenType.EOF);
        assertEquals(0.0, (Double) tokens.get(0).literal(), 1e-9);
        assertEquals(0.5, (Double) tokens.get(1).literal(), 1e-9);
        assertEquals(3.14, (Double) tokens.get(2).literal(), 1e-9);
        assertEquals(3.0, (Double) tokens.get(3).literal(), 1e-9);
    }
}
