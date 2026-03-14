package src.errors;

import org.junit.jupiter.api.Test;
import src.Scanner;
import src.ScannerTestBase;

import static org.junit.jupiter.api.Assertions.*;

class UnclosedStringsTest extends ScannerTestBase {

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

    @Test
    void unterminatedChar_alone() {
        assertThrowsLexical(
                () -> new Scanner("'").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }

    @Test
    void unterminatedChar_noClosingQuote() {
        assertThrowsLexical(
                () -> new Scanner("'a").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }

    @Test
    void unterminatedChar_backslashAtEof() {
        assertThrowsLexical(
                () -> new Scanner("'\\").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }

    @Test
    void unterminatedChar_newlineBeforeClosing() {
        assertThrowsLexical(
                () -> new Scanner("'a\n").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }

    @Test
    void emptyChar_throwsUnterminated() {
        assertThrowsLexical(
                () -> new Scanner("''").tokenizeAll(),
                "Unterminated character constant", 1, 1);
    }
}
