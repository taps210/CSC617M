package src;

import src.errors.ParseException;
import org.junit.jupiter.api.function.Executable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Shared helpers for parser tests. */
public abstract class ParserTestBase {

    protected static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    protected static final Path TESTS_IN = PROJECT_ROOT.resolve("tests").resolve("inputs");
    protected static final Path TESTS_OUT = PROJECT_ROOT.resolve("tests").resolve("outputs");

    /** Tokenize source and run parser, return full trace string (including "Parse OK"). */
    protected String parseToTrace(String src) {
        List<Token> tokens = new Scanner(src).tokenizeAll();
        StringBuilder out = new StringBuilder();
        Parser parser = new Parser(tokens, out);
        parser.parseProgram();
        out.append("Parse OK").append(System.lineSeparator());
        return out.toString();
    }

    /** Run parser on source; assert no exception. */
    protected void assertParseSuccess(String src) {
        List<Token> tokens = new Scanner(src).tokenizeAll();
        Parser parser = new Parser(tokens, new StringBuilder());
        assertDoesNotThrow(parser::parseProgram);
    }

    /** Run parser on source; expect ParseException with message containing substring at given line/column. */
    protected void parseExpectError(String src, String messageSubstring, int expectedLine, int expectedCol) {
        assertThrowsParse(
                () -> {
                    List<Token> tokens = new Scanner(src).tokenizeAll();
                    new Parser(tokens, new StringBuilder()).parseProgram();
                },
                messageSubstring,
                expectedLine,
                expectedCol
        );
    }

    /** Assert executable throws ParseException with message containing substring and correct line/column. */
    protected void assertThrowsParse(Executable executable, String messageSubstring, int expectedLine, int expectedCol) {
        ParseException e = assertThrows(ParseException.class, executable);
        String msg = e.getMessage();
        assertTrue(msg.contains(messageSubstring), "Message should contain: " + messageSubstring + ", got: " + msg);
        assertEquals(expectedLine, e.line(), "Line");
        assertEquals(expectedCol, e.col(), "Column");
    }

    protected Path testsIn() {
        return TESTS_IN;
    }

    protected Path testsOut() {
        return TESTS_OUT;
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
