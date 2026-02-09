package src.errors;

import org.junit.jupiter.api.Test;
import src.LexicalErrorRecord;
import src.Scanner;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies scanner recovery mode: error list and optional stdout. */
class RecoveryTest extends ScannerTestBase {

    private static String captureStdout(Runnable action) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream prev = System.out;
        System.setOut(new PrintStream(stdout));
        try {
            action.run();
            return stdout.toString();
        } finally {
            System.setOut(prev);
        }
    }

    @Test
    void numberThenLetter_withRecoveryMessages_printsAndProducesTokens() {
        AtomicReference<List<Token>> tokensRef = new AtomicReference<>();
        String out = captureStdout(() -> tokensRef.set(new Scanner("12a").tokenizeAll(true)));
        List<Token> tokens = tokensRef.get();
        assertTrue(out.contains("Error found in line 1 column"), "Should print error line/col: " + out);
        assertTrue(out.contains("Recovering"), "Should print recovery message: " + out);
        assertEquals(2, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals("12", tokens.get(0).lexeme());
        assertEquals(12, tokens.get(0).literal());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void numberThenLetter_withoutRecoveryMessages_sameTokensNoStdout() {
        AtomicReference<List<Token>> tokensRef = new AtomicReference<>();
        String out = captureStdout(() -> tokensRef.set(new Scanner("12a").tokenizeAll(false)));
        assertEquals("", out);
        List<Token> tokens = tokensRef.get();
        assertEquals(2, tokens.size());
        assertEquals(TokenType.INT_LIT, tokens.get(0).type());
        assertEquals(TokenType.EOF, tokens.get(1).type());
    }

    @Test
    void unknownSymbol_withErrorCollector_addsErrorAndContinues() {
        List<LexicalErrorRecord> errors = new ArrayList<>();
        List<Token> tokens = new Scanner("a # b").tokenizeAll(false, errors);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).message().contains("#"));
        assertEquals(TokenType.IDENT, tokens.get(0).type());
        assertEquals(TokenType.IDENT, tokens.get(1).type());
        assertEquals(TokenType.EOF, tokens.get(2).type());
    }
}
