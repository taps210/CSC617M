package src.operators;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.TokenType;

import static org.junit.jupiter.api.Assertions.*;

class ArithmeticTest extends ScannerTestBase {

    @Test
    void plus() {
        assertTokenTypes(tokenize("+"), TokenType.PLUS, TokenType.EOF);
    }

    @Test
    void minus() {
        assertTokenTypes(tokenize("-"), TokenType.MINUS, TokenType.EOF);
    }

    @Test
    void star() {
        assertTokenTypes(tokenize("*"), TokenType.STAR, TokenType.EOF);
    }

    @Test
    void slash() {
        assertTokenTypes(tokenize("/"), TokenType.SLASH, TokenType.EOF);
    }

    @Test
    void mod() {
        assertTokenTypes(tokenize("%"), TokenType.MOD, TokenType.EOF);
    }

    @Test
    void allArithmetic() {
        assertTokenTypes(tokenize("+ - * / %"),
                TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.MOD, TokenType.EOF);
    }

    @Test
    void expression_xPlusY() {
        assertTokenTypes(tokenize("x + y"),
                TokenType.IDENT, TokenType.PLUS, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void expression_aTimesBMinusC() {
        assertTokenTypes(tokenize("a * b - c"),
                TokenType.IDENT, TokenType.STAR, TokenType.IDENT, TokenType.MINUS, TokenType.IDENT, TokenType.EOF);
    }
}
