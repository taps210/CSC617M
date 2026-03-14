package src.literals;

import org.junit.jupiter.api.Test;
import src.Scanner;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CharsTest extends ScannerTestBase {

    @Test
    void singleChar_a() {
        List<Token> tokens = tokenize("'a'");
        assertTokenTypes(tokens, TokenType.CHAR_LIT, TokenType.EOF);
        assertEquals('a', tokens.get(0).literal());
    }

    @Test
    void singleChar_Z() {
        List<Token> tokens = tokenize("'Z'");
        assertEquals('Z', tokens.get(0).literal());
    }

    @Test
    void digitChar_0() {
        List<Token> tokens = tokenize("'0'");
        assertEquals('0', tokens.get(0).literal());
    }

    @Test
    void spaceChar() {
        List<Token> tokens = tokenize("' '");
        assertEquals(' ', tokens.get(0).literal());
    }

    @Test
    void newlineEscape() {
        List<Token> tokens = tokenize("'\\n'");
        assertEquals('\n', tokens.get(0).literal());
    }

    @Test
    void tabEscape() {
        List<Token> tokens = tokenize("'\\t'");
        assertEquals('\t', tokens.get(0).literal());
    }

    @Test
    void backslashEscape() {
        List<Token> tokens = tokenize("'\\\\'");
        assertEquals('\\', tokens.get(0).literal());
    }

    @Test
    void singleQuoteEscape() {
        List<Token> tokens = tokenize("'\\''");
        assertEquals('\'', tokens.get(0).literal());
    }

    @Test
    void doubleQuoteInChar() {
        List<Token> tokens = tokenize("'\"'");
        assertEquals('"', tokens.get(0).literal());
    }

    @Test
    void multipleChars() {
        List<Token> tokens = tokenize("'a' '\\n' '\\'' '\"'");
        assertTokenTypes(tokens, TokenType.CHAR_LIT, TokenType.CHAR_LIT, TokenType.CHAR_LIT, TokenType.CHAR_LIT, TokenType.EOF);
        assertEquals('a', tokens.get(0).literal());
        assertEquals('\n', tokens.get(1).literal());
        assertEquals('\'', tokens.get(2).literal());
        assertEquals('"', tokens.get(3).literal());
    }

    @Test
    void emptyChar_throwsUnterminated() {
        assertThrowsLexical(
                () -> new Scanner("''").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }

    @Test
    void multiChar_ab_throwsOrMalformed() {
        assertThrowsLexical(
                () -> new Scanner("'ab'").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }

    @Test
    void unclosedChar_alone() {
        assertThrowsLexical(
                () -> new Scanner("'").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }

    @Test
    void unclosedChar_noClosingQuote() {
        assertThrowsLexical(
                () -> new Scanner("'a").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }
}
