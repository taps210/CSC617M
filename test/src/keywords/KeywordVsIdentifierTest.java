package src.keywords;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.TokenType;

import static org.junit.jupiter.api.Assertions.*;

class KeywordVsIdentifierTest extends ScannerTestBase {

    @Test
    void ifStatement_isIdentifierNotKeyword() {
        assertTokenTypes(tokenize("ifStatement"),
                TokenType.IDENT, TokenType.EOF);
        assertEquals("ifStatement", tokenize("ifStatement").get(0).lexeme());
    }

    @Test
    void integer_isIdentifierNotKeyword() {
        assertTokenTypes(tokenize("integer"),
                TokenType.IDENT, TokenType.EOF);
        assertEquals("integer", tokenize("integer").get(0).lexeme());
    }

    @Test
    void ifFollowedByIdentifier() {
        assertTokenTypes(tokenize("if x"),
                TokenType.IF, TokenType.IDENT, TokenType.EOF);
    }

    @Test
    void mainFunction_asIdentifierSuffix() {
        assertTokenTypes(tokenize("my_main"),
                TokenType.IDENT, TokenType.EOF);
        assertEquals("my_main", tokenize("my_main").get(0).lexeme());
    }

    @Test
    void typeName_asIdentifier() {
        assertTokenTypes(tokenize("typeName"),
                TokenType.IDENT, TokenType.EOF);
    }
}
