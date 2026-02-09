package src.operators;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.TokenType;

import static org.junit.jupiter.api.Assertions.*;

class MaximalMunchTest extends ScannerTestBase {

    @Test
    void aLessOrEqualB() {
        assertTokenTypes(tokenize("a<=b"),
                TokenType.IDENT, TokenType.LTE, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void xNotEqualY() {
        assertTokenTypes(tokenize("x!=y"),
                TokenType.IDENT, TokenType.NEQ, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void lessOrEqual_notTwoTokens() {
        assertTokenTypes(tokenize("<="), TokenType.LTE, TokenType.EOF);
    }

    @Test
    void greaterOrEqual_notTwoTokens() {
        assertTokenTypes(tokenize(">="), TokenType.GTE, TokenType.EOF);
    }

    @Test
    void equalEqual_notTwoAssigns() {
        assertTokenTypes(tokenize("=="), TokenType.EQEQ, TokenType.EOF);
    }

    @Test
    void andAnd_notTwoSingleChars() {
        assertTokenTypes(tokenize("&&"), TokenType.ANDAND, TokenType.EOF);
    }

    @Test
    void orOr_notTwoSingleChars() {
        assertTokenTypes(tokenize("||"), TokenType.OROR, TokenType.EOF);
    }

    @Test
    void comparisonChain() {
        assertTokenTypes(tokenize("a<=b&&c!=d"),
                TokenType.IDENT, TokenType.LTE, TokenType.IDENT, TokenType.ANDAND, TokenType.IDENT, TokenType.NEQ, TokenType.IDENT, TokenType.EOF);
    }
}
