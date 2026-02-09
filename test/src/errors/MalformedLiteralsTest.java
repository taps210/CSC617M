package src.errors;

import org.junit.jupiter.api.Test;
import src.Scanner;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Malformed literals: 123abc (scanner recovers: INT_LIT + IDENT),
 * 'ab' (throws), 1.2.3 (scanner may tokenize 1.2 then .3 or error).
 */
class MalformedLiteralsTest extends ScannerTestBase {

    @Test
    void numberThenLetter_123abc_recoversAsIntThenIdent() {
        List<Token> tokens = tokenize("123abc");
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(123, tokens.get(0).literal());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals("bc", tokens.get(1).lexeme());
    }

    @Test
    void multiCharInSingleQuotes_ab_throws() {
        assertThrowsLexical(
                () -> new Scanner("'ab'").tokenizeAll(false),
                "Unterminated character constant", 1, 1);
    }

    @Test
    void multipleDots_1_2_3_floatThenFloat() {
        List<Token> tokens = tokenize("1.2.3");
        assertEquals(3, tokens.size());
        assertEquals(TokenType.FLOAT_LIT, tokens.get(0).type());
        assertEquals(1.2, (Double) tokens.get(0).literal(), 1e-9);
        assertEquals(TokenType.FLOAT_LIT, tokens.get(1).type());
        assertEquals(0.3, (Double) tokens.get(1).literal(), 1e-9);
        assertEquals(TokenType.EOF, tokens.get(2).type());
    }
}
