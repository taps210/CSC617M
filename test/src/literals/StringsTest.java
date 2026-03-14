package src.literals;

import org.junit.jupiter.api.Test;
import src.Scanner;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringsTest extends ScannerTestBase {

    @Test
    void hello() {
        List<Token> tokens = tokenize("\"hello\"");
        assertTokenTypes(tokens, TokenType.STRING_LIT, TokenType.EOF);
        assertEquals("hello", tokens.get(0).literal());
    }

    @Test
    void helloWorld() {
        List<Token> tokens = tokenize("\"Hello, World!\"");
        assertEquals("Hello, World!", tokens.get(0).literal());
    }

    @Test
    void emptyString() {
        List<Token> tokens = tokenize("\"\"");
        assertEquals(TokenType.STRING_LIT, tokens.get(0).type());
        assertEquals("", tokens.get(0).literal());
    }

    @Test
    void escapedQuotes() {
        List<Token> tokens = tokenize("\"She said \\\"Hi\\\"\"");
        assertNotNull(tokens.get(0).literal());
        assertTrue(tokens.get(0).literal().toString().contains("Hi"));
    }

    @Test
    void escapeSequences() {
        List<Token> tokens = tokenize("\"\\\\ \\\" \\' \\n \\r \\t\"");
        assertEquals(TokenType.STRING_LIT, tokens.get(0).type());
        assertNotNull(tokens.get(0).literal());
    }

    @Test
    void line1NewlineLine2() {
        List<Token> tokens = tokenize("\"Line 1\\nLine 2\"");
        assertNotNull(tokens.get(0).literal());
    }

    @Test
    void tabInString() {
        List<Token> tokens = tokenize("\"Tab\\there\"");
        assertNotNull(tokens.get(0).literal());
    }

    @Test
    void multipleStrings() {
        List<Token> tokens = tokenize("\"\" \"a\" \"hello\"");
        assertTokenTypes(tokens, TokenType.STRING_LIT, TokenType.STRING_LIT, TokenType.STRING_LIT, TokenType.EOF);
        assertEquals("", tokens.get(0).literal());
        assertEquals("a", tokens.get(1).literal());
        assertEquals("hello", tokens.get(2).literal());
    }

    @Test
    void unterminatedString_noClosingQuote() {
        assertThrowsLexical(
                () -> new Scanner("\"hello").tokenizeAll(),
                "Unterminated string constant", 1, 1);
    }

    @Test
    void unterminatedString_newlineBeforeClosing() {
        assertThrowsLexical(
                () -> new Scanner("\"hello\n").tokenizeAll(),
                "Unterminated string constant", 1, 1);
    }

    @Test
    void unterminatedString_backslashAtEof() {
        assertThrowsLexical(
                () -> new Scanner("\"x\\").tokenizeAll(),
                "Unterminated string constant", 1, 1);
    }
}
