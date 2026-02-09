package src.whitespace;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WhitespaceHandlingTest extends ScannerTestBase {

    @Test
    void singleSpace() {
        assertTokenTypes(tokenize("int x;"),
                TokenType.INT, TokenType.IDENT, TokenType.SEMI, TokenType.EOF);
    }

    @Test
    void multipleSpaces() {
        assertTokenTypes(tokenize("int    x;"),
                TokenType.INT, TokenType.IDENT, TokenType.SEMI, TokenType.EOF);
    }

    @Test
    void tabBetweenTokens() {
        assertTokenTypes(tokenize("int\tx;"),
                TokenType.INT, TokenType.IDENT, TokenType.SEMI, TokenType.EOF);
    }

    @Test
    void multipleNewlines() {
        List<Token> tokens = tokenize("int\n\nx;");
        assertTokenTypes(tokens, TokenType.INT, TokenType.IDENT, TokenType.SEMI, TokenType.EOF);
        assertEquals(1, tokens.get(0).line());
        assertEquals(3, tokens.get(1).line());
    }

    @Test
    void windowsLineEndings() {
        List<Token> tokens = tokenize("int\r\nx;");
        assertTokenTypes(tokens, TokenType.INT, TokenType.IDENT, TokenType.SEMI, TokenType.EOF);
        assertEquals(1, tokens.get(0).line());
        assertEquals(2, tokens.get(1).line());
    }

    @Test
    void lineAndColumnPositions() {
        String src = "a\nbb\nccc";
        List<Token> tokens = tokenize(src);
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals(1, tokens.get(0).line());
        assertEquals(1, tokens.get(0).col());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals(2, tokens.get(1).line());
        assertEquals(1, tokens.get(1).col());
        assertEquals(TokenType.IDENT, tokens.get(2).type());
        assertEquals(3, tokens.get(2).line());
        assertEquals(1, tokens.get(2).col());
        assertEquals(TokenType.EOF, tokens.get(3).type());
        assertEquals(3, tokens.get(3).line());
        assertEquals(4, tokens.get(3).col());
    }

    @Test
    void emptyInput_onlyEof() {
        List<Token> tokens = tokenize("");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type());
        assertEquals(1, tokens.get(0).line());
        assertEquals(1, tokens.get(0).col());
    }
}
