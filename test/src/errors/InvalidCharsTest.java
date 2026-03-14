package src.errors;

import org.junit.jupiter.api.Test;
import src.Scanner;
import src.ScannerTestBase;

import static org.junit.jupiter.api.Assertions.*;

class InvalidCharsTest extends ScannerTestBase {

    @Test
    void unknownSymbol_hash() {
        assertThrowsLexical(
                () -> new Scanner("#").tokenizeAll(),
                "Unknown symbol \"#\"", 1, 1);
    }

    @Test
    void unknownSymbol_at() {
        assertThrowsLexical(
                () -> new Scanner("@").tokenizeAll(),
                "Unknown symbol \"@\"", 1, 1);
    }

    @Test
    void unknownSymbol_dollar() {
        assertThrowsLexical(
                () -> new Scanner("$").tokenizeAll(),
                "Unknown symbol \"$\"", 1, 1);
    }

    @Test
    void unknownSymbol_withContext_line2() {
        assertThrowsLexical(
                () -> new Scanner("a\n#").tokenizeAll(),
                "Unknown symbol \"#\"", 2, 1);
    }

    @Test
    void lonePipe() {
        assertThrowsLexical(
                () -> new Scanner("|").tokenizeAll(),
                "Unknown symbol \"|\"", 1, 1);
    }

    @Test
    void lonePipeWithContext_didYouMeanOrOr() {
        assertThrowsLexical(
                () -> new Scanner("a | b").tokenizeAll(),
                "did you mean \"||\"?", 1, 3);
    }
}
