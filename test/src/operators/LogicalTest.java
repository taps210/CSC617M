package src.operators;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.TokenType;

import static org.junit.jupiter.api.Assertions.*;

class LogicalTest extends ScannerTestBase {

    @Test
    void andAnd() {
        assertTokenTypes(tokenize("&&"), TokenType.ANDAND, TokenType.EOF);
    }

    @Test
    void orOr() {
        assertTokenTypes(tokenize("||"), TokenType.OROR, TokenType.EOF);
    }

    @Test
    void not() {
        assertTokenTypes(tokenize("!"), TokenType.NOT, TokenType.EOF);
    }

    @Test
    void distinguish_notVsNotEqual() {
        assertTokenTypes(tokenize("! !="), TokenType.NOT, TokenType.NEQ, TokenType.EOF);
    }

    @Test
    void assignVsEqual() {
        assertTokenTypes(tokenize("= =="), TokenType.ASSIGN, TokenType.EQEQ, TokenType.EOF);
    }

    @Test
    void logicalExpression() {
        assertTokenTypes(tokenize("a && b || !c"),
                TokenType.IDENT, TokenType.ANDAND, TokenType.IDENT, TokenType.OROR, TokenType.NOT, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void ternary_qmarkColon() {
        assertTokenTypes(tokenize("? :"), TokenType.QMARK, TokenType.COLON, TokenType.EOF);
    }

    @Test
    void flagTernary() {
        assertTokenTypes(tokenize("flag ? 1 : 0"),
                TokenType.IDENT, TokenType.QMARK, TokenType.INT_LIT, TokenType.COLON, TokenType.INT_LIT, TokenType.EOF);
    }
}
