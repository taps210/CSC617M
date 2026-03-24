package src.errors;

import org.junit.jupiter.api.Test;
import src.Scanner;
import src.ScannerTestBase;
import src.Token;
import src.TokenType;

import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void multipleRecoverableErrors_inFile_collectsAllAndContinuesTokenizing() throws Exception {
        Path input = testsIn().resolve("LexRecovery_Multiple.txt");
        assumeFilesExist(input);

        List<LexicalErrorRecord> errors = new ArrayList<>();
        List<Token> tokens = new Scanner(Files.readString(input)).tokenizeAll(errors);

        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).type(), "Should still reach EOF");
        assertTrue(errors.size() >= 3, "Should record multiple recovery errors (got " + errors.size() + ")");

        boolean sawNumberRecovery = errors.stream().anyMatch(e -> e.message().contains("Recovering:"));
        boolean sawSinglePipe = errors.stream().anyMatch(e -> e.message().contains("did you mean \"||\""));
        boolean sawHash = errors.stream().anyMatch(e -> e.message().contains("#"));

        assertTrue(sawNumberRecovery, "Expected number+letter recovery message");
        assertTrue(sawSinglePipe, "Expected single '|' recovery message");
        assertTrue(sawHash, "Expected unknown symbol '#' message");
    }
}
