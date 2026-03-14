package src.errors;

import org.junit.jupiter.api.Test;
import src.Scanner;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies scanner recovery mode: error list populated, scanning continues. */
class RecoveryTest extends ScannerTestBase {

    @Test
    void numberThenLetter_withErrorCollector_capturesRecoveryError() {
        List<LexicalErrorRecord> errors = new ArrayList<>();
        List<Token> tokens = new Scanner("12a").tokenizeAll(errors);
        assertFalse(errors.isEmpty(), "Should record recovery error");
        assertTrue(errors.get(0).message().contains("Recovering"), "Error message should describe recovery: " + errors.get(0).message());
        assertEquals(2, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals("12", tokens.get(0).lexeme());
        assertEquals(12, tokens.get(0).literal());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void numberThenLetter_withoutErrorCollector_producesTokensSilently() {
        List<Token> tokens = new Scanner("12a").tokenizeAll();
        assertEquals(2, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void unknownSymbol_withErrorCollector_addsErrorAndContinues() {
        List<LexicalErrorRecord> errors = new ArrayList<>();
        List<Token> tokens = new Scanner("a # b").tokenizeAll(errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).message().contains("#"));
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals(TokenType.EOF, tokens.get(2).type());
    }
}
