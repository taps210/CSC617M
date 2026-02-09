package src.comments;

import org.junit.jupiter.api.Test;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SingleLineCommentTest extends ScannerTestBase {

    @Test
    void commentIgnored() {
        List<Token> tokens = tokenize("// This is a comment\n42");
        assertEquals(2, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(42, tokens.get(0).literal());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void codeAfterCommentOnSameLine() {
        List<Token> tokens = tokenize("int x; // comment after code");
        assertTokenTypes(tokens, TokenType.INT, TokenType.IDENT, TokenType.SEMI, TokenType.EOF);
    }

    @Test
    void entireLineComment() {
        List<Token> tokens = tokenize("// only comment");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type());
    }

    @Test
    void multipleLineComments() {
        List<Token> tokens = tokenize("// one\n// two\nx");
        assertTokenTypes(tokens, TokenType.IDENT, TokenType.EOF);
        assertEquals("x", tokens.get(0).lexeme());
    }
}
