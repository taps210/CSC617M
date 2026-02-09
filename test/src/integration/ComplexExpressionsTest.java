package src.integration;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.TokenType;

import static org.junit.jupiter.api.Assertions.*;

class ComplexExpressionsTest extends ScannerTestBase {

    @Test
    void expression_xPlusYTimesZ() {
        assertTokenTypes(tokenize("x + y * z"),
                TokenType.IDENT, TokenType.PLUS, TokenType.IDENT, TokenType.STAR, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void expression_aLessOrEqualBAndCNotEqualD() {
        assertTokenTypes(tokenize("a <= b && c != d"),
                TokenType.IDENT, TokenType.LTE, TokenType.IDENT, TokenType.ANDAND, TokenType.IDENT, TokenType.NEQ, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void ternary_flagQuestion1Colon0() {
        assertTokenTypes(tokenize("flag ? 1 : 0"),
                TokenType.IDENT, TokenType.QMARK, TokenType.INT_LIT, TokenType.COLON, TokenType.INT_LIT, TokenType.EOF);
    }

    @Test
    void notIsValidOrHasError() {
        assertTokenTypes(tokenize("!isValid || hasError"),
                TokenType.NOT, TokenType.IDENT, TokenType.OROR, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void assignment_xEquals5() {
        assertTokenTypes(tokenize("x = 5;"),
                TokenType.IDENT, TokenType.ASSIGN, TokenType.INT_LIT, TokenType.SEMI, TokenType.EOF);
    }

    @Test
    void arrayIndexAssignment() {
        assertTokenTypes(tokenize("arr[0] = 10;"),
                TokenType.IDENT, TokenType.LBRACKET, TokenType.INT_LIT, TokenType.RBRACKET, TokenType.ASSIGN, TokenType.INT_LIT, TokenType.SEMI, TokenType.EOF);
    }

    @Test
    void agentFieldAccess() {
        assertTokenTypes(tokenize("obj.x = 20;"),
                TokenType.IDENT, TokenType.DOT, TokenType.IDENT, TokenType.ASSIGN, TokenType.INT_LIT, TokenType.SEMI, TokenType.EOF);
    }

    @Test
    void ifStatement() {
        assertTokenTypes(tokenize("if (x > 0) { }"),
                TokenType.IF, TokenType.LPAREN, TokenType.IDENT, TokenType.GT, TokenType.INT_LIT, TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE, TokenType.EOF);
    }

    @Test
    void whileLoop() {
        assertTokenTypes(tokenize("while (i < 10) { i = i + 1; }"),
                TokenType.WHILE, TokenType.LPAREN, TokenType.IDENT, TokenType.LT, TokenType.INT_LIT, TokenType.RPAREN, TokenType.LBRACE,
                TokenType.IDENT, TokenType.ASSIGN, TokenType.IDENT, TokenType.PLUS, TokenType.INT_LIT, TokenType.SEMI, TokenType.RBRACE, TokenType.EOF);
    }

    @Test
    void complexArithmeticExpression() {
        assertTokenTypes(tokenize("(a + b) * c - d / e % f"),
                TokenType.LPAREN, TokenType.IDENT, TokenType.PLUS, TokenType.IDENT, TokenType.RPAREN, TokenType.STAR, TokenType.IDENT, TokenType.MINUS,
                TokenType.IDENT, TokenType.SLASH, TokenType.IDENT, TokenType.MOD, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void ifWithComplexCondition() {
        assertTokenTypes(tokenize("if (result >= 0 && result <= 100) { print(result); }"),
                TokenType.IF, TokenType.LPAREN, TokenType.IDENT, TokenType.GTE, TokenType.INT_LIT, TokenType.ANDAND, TokenType.IDENT, TokenType.LTE, TokenType.INT_LIT, TokenType.RPAREN,
                TokenType.LBRACE, TokenType.PRINT, TokenType.LPAREN, TokenType.IDENT, TokenType.RPAREN, TokenType.SEMI, TokenType.RBRACE, TokenType.EOF);
    }
}
