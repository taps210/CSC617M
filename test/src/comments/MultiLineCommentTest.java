package src.comments;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiLineCommentTest extends ScannerTestBase {

    @Test
    void blockComment_skipped() {
        List<Token> tokens = tokenize("/* block */ 1");
        assertEquals(2, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(1, tokens.get(0).literal());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void blockCommentAcrossLines() {
        List<Token> tokens = tokenize("/* line1\nline2 */ x");
        assertEquals(2, tokens.size());
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals("x", tokens.get(0).lexeme());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void commentsOnly_thenEof() {
        List<Token> tokens = tokenize("// only comment\n/* block */");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type());
    }

    @Test
    void unterminatedBlockComment_consumesToEofThenEofToken() {
        List<Token> tokens = tokenize("/* no close");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type());
    }
}
