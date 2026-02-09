package src.operators;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.TokenType;

import static org.junit.jupiter.api.Assertions.*;

class RelationalTest extends ScannerTestBase {

    @Test
    void lessThan() {
        assertTokenTypes(tokenize("<"), TokenType.LT, TokenType.EOF);
    }

    @Test
    void lessOrEqual() {
        assertTokenTypes(tokenize("<="), TokenType.LTE, TokenType.EOF);
    }

    @Test
    void greaterThan() {
        assertTokenTypes(tokenize(">"), TokenType.GT, TokenType.EOF);
    }

    @Test
    void greaterOrEqual() {
        assertTokenTypes(tokenize(">="), TokenType.GTE, TokenType.EOF);
    }

    @Test
    void equalEqual() {
        assertTokenTypes(tokenize("=="), TokenType.EQEQ, TokenType.EOF);
    }

    @Test
    void notEqual() {
        assertTokenTypes(tokenize("!="), TokenType.NEQ, TokenType.EOF);
    }

    @Test
    void distinguish_lessThanVsLessOrEqual() {
        assertTokenTypes(tokenize("< <="), TokenType.LT, TokenType.LTE, TokenType.EOF);
    }

    @Test
    void distinguish_greaterThanVsGreaterOrEqual() {
        assertTokenTypes(tokenize("> >="), TokenType.GT, TokenType.GTE, TokenType.EOF);
    }

    @Test
    void allRelational() {
        assertTokenTypes(tokenize("< <= > >= == !="),
                TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE, TokenType.EQEQ, TokenType.NEQ, TokenType.EOF);
    }
}
