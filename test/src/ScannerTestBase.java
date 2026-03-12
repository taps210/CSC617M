package src;

import src.errors.LexicalErrorRecord;
import org.junit.jupiter.api.function.Executable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Shared helpers for scanner tests. */
public abstract class ScannerTestBase {

    protected static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    protected static final Path TESTS_IN = PROJECT_ROOT.resolve("tests").resolve("inputs");
    protected static final Path TESTS_OUT = PROJECT_ROOT.resolve("tests").resolve("outputs");

    protected List<Token> tokenize(String src) {
        return new Scanner(src).tokenizeAll(false);
    }

    protected void assertTokenTypes(List<Token> tokens, TokenType... expected) {
        assertEquals(expected.length, tokens.size(), "Token count");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).type(), "at index " + i);
        }
    }

    protected void assertThrowsLexical(Executable executable, String messageSubstring, int expectedLine, int expectedCol) {
        LexicalErrorRecord.ScanAbortedException e = assertThrows(LexicalErrorRecord.ScanAbortedException.class, executable);
        LexicalErrorRecord err = e.getError();
        assertNotNull(err, "ScanAbortedException should carry an error when thrown without a list");
        String msg = e.getMessage();
        assertTrue(msg.contains(messageSubstring), "Message should contain: " + messageSubstring + ", got: " + msg);
        assertTrue(msg.contains("line " + expectedLine), "Message should contain line " + expectedLine + ", got: " + msg);
        assertTrue(msg.contains("column " + expectedCol), "Message should contain column " + expectedCol + ", got: " + msg);
        assertEquals(expectedLine, err.line());
        assertEquals(expectedCol, err.col());
    }

    protected Path testsIn() {
        return TESTS_IN;
    }

    protected Path testsOut() {
        return TESTS_OUT;
    }

    protected String tokenDumpFromFile(Path input) throws Exception {
        List<Token> tokens = new Scanner(Files.readString(input)).tokenizeAll(false);
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(t.toString()).append("\n");
        }
        return sb.toString();
    }

    protected static String normalizeLines(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

    protected static void assumeFilesExist(Path... paths) {
        for (Path p : paths) {
            if (!Files.exists(p)) {
                throw new AssertionError("Missing file: " + p.toAbsolutePath());
            }
        }
    }
}
