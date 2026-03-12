package src;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Compares scanner output to golden files under tests/inputs and tests/outputs. */
class GoldenFileTest {

    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path TESTS_IN = PROJECT_ROOT.resolve("tests").resolve("inputs");
    private static final Path TESTS_OUT = PROJECT_ROOT.resolve("tests").resolve("outputs");

    @Test
    void fullTokenDump_matchesGolden() throws Exception {
        Path input = TESTS_IN.resolve("FullTokenDump.txt");
        Path expected = TESTS_OUT.resolve("FullTokenDump_Output.txt");
        assumeFilesExist(input, expected);
        assertEquals(normalizeLines(Files.readString(expected)), normalizeLines(tokenDumpFromFile(input)));
    }

    @Test
    void test1_matchesGolden() throws Exception {
        Path input = TESTS_IN.resolve("Test1.txt");
        Path expected = TESTS_OUT.resolve("Test1_Output.txt");
        assumeFilesExist(input, expected);
        assertEquals(normalizeLines(Files.readString(expected)), normalizeLines(tokenDumpFromFile(input)));
    }

    @Test
    void lexError_unknownSymbol_matchesGolden() throws Exception {
        Path input = TESTS_IN.resolve("LexError_UnknownSymbol.txt");
        Path expected = TESTS_OUT.resolve("LexError_UnknownSymbol_Output.txt");
        assumeFilesExist(input, expected);

        String src = Files.readString(input);
        LexicalErrorRecord.ScanAbortedException e = assertThrows(LexicalErrorRecord.ScanAbortedException.class, () -> new Scanner(src).tokenizeAll(false));
        String expectedStr = Files.readString(expected);
        assertEquals(normalizeLines(expectedStr), normalizeLines(e.getMessage()));
    }

    @Test
    void lexError_unterminatedString_matchesGolden() throws Exception {
        Path input = TESTS_IN.resolve("LexError_UnterminatedString.txt");
        Path expected = TESTS_OUT.resolve("LexError_UnterminatedString_Output.txt");
        assumeFilesExist(input, expected);

        String src = Files.readString(input);
        LexicalErrorRecord.ScanAbortedException e = assertThrows(LexicalErrorRecord.ScanAbortedException.class, () -> new Scanner(src).tokenizeAll(false));
        String expectedStr = Files.readString(expected);
        assertEquals(normalizeLines(expectedStr), normalizeLines(e.getMessage()));
    }

    @Test
    void sample01_marketplace_matchesGolden() throws Exception {
        goldenSample("Sample01_Marketplace.txt", "Sample01_Output.txt");
    }

    @Test
    void sample02_trafficNeighbors_matchesGolden() throws Exception {
        goldenSample("Sample02_TrafficNeighbors.txt", "Sample02_Output.txt");
    }

    @Test
    void sample02_trafficZone_matchesGolden() throws Exception {
        goldenSample("Sample02_TrafficZone.txt", "Sample02_TrafficZone_Output.txt");
    }

    @Test
    void sample03_trafficZone_matchesGolden() throws Exception {
        goldenSample("Sample03_TrafficZone.txt", "Sample03_Output.txt");
    }

    @Test
    void sample04_diseaseSpread_matchesGolden() throws Exception {
        goldenSample("Sample04_DiseaseSpread.txt", "Sample04_Output.txt");
    }

    @Test
    void sample05_resourceCompetition_matchesGolden() throws Exception {
        goldenSample("Sample05_ResourceCompetition.txt", "Sample05_Output.txt");
    }

    @Test
    void largeFile_correct_tokenCountMatchesRecorded() throws Exception {
        Path input = TESTS_IN.resolve("LargeFile.txt");
        Path statsFile = TESTS_OUT.resolve("LargeFile_BenchStats.txt");
        assumeFilesExist(input, statsFile);
        List<Token> tokens = new Scanner(Files.readString(input)).tokenizeAll(false);
        assertEquals(parseFirstLineValue(statsFile, "Tokens:"), tokens.size(),
                "Token count should match recorded value in LargeFile_BenchStats.txt");
    }

    @Test
    void largeFile_small_tokenCountMatchesRecorded() throws Exception {
        Path input = TESTS_IN.resolve("LargeFile_Small.txt");
        Path statsFile = TESTS_OUT.resolve("LargeFile_Small_BenchStats.txt");
        assumeFilesExist(input, statsFile);
        List<Token> tokens = new Scanner(Files.readString(input)).tokenizeAll(false);
        assertEquals(parseFirstLineValue(statsFile, "Tokens:"), tokens.size(),
                "Token count should match recorded value in LargeFile_Small_BenchStats.txt");
    }

    @Test
    void largeFile_large_tokenCountMatchesRecorded() throws Exception {
        Path input = TESTS_IN.resolve("LargeFile_Large.txt");
        Path statsFile = TESTS_OUT.resolve("LargeFile_Large_BenchStats.txt");
        assumeFilesExist(input, statsFile);
        List<Token> tokens = new Scanner(Files.readString(input)).tokenizeAll(false);
        assertEquals(parseFirstLineValue(statsFile, "Tokens:"), tokens.size(),
                "Token count should match recorded value in LargeFile_Large_BenchStats.txt");
    }

    // @Test
    // void largeFile_withErrors_matchesGolden() throws Exception {
    //     Path input = TESTS_IN.resolve("LargeFile_WithErrors.txt");
    //     Path expectedOutput = TESTS_OUT.resolve("LargeFile_WithErrors_Output.txt");
    //     Path statsFile = TESTS_OUT.resolve("LargeFile_WithErrors_Stats.txt");
    //     assumeFilesExist(input, expectedOutput, statsFile);

    //     var errors = new ArrayList<LexicalErrorRecord>();
    //     new Scanner(Files.readString(input)).tokenizeAll(false, errors);
    //     assertEquals(parseFirstLineValue(statsFile, "Total errors:"), errors.size(),
    //             "Error count should match LargeFile_WithErrors_Stats.txt");

    //     String actual = String.join(System.lineSeparator(), errors.stream().map(LexicalErrorRecord::format).toList());
    //     assertEquals(normalizeLines(Files.readString(expectedOutput)), normalizeLines(actual),
    //             "Full error list should match LargeFile_WithErrors_Output.txt");
    // }

    private static void goldenSample(String inputName, String outputName) throws Exception {
        Path input = TESTS_IN.resolve(inputName);
        Path expected = TESTS_OUT.resolve(outputName);
        assumeFilesExist(input, expected);
        assertEquals(normalizeLines(Files.readString(expected)), normalizeLines(tokenDumpFromFile(input)));
    }

    private static String tokenDumpFromFile(Path input) throws Exception {
        List<Token> tokens = new Scanner(Files.readString(input)).tokenizeAll(false);
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(t.toString()).append("\n");
        }
        return sb.toString();
    }

    private static int parseFirstLineValue(Path statsFile, String prefix) throws Exception {
        String line = Files.readString(statsFile).lines()
                .filter(l -> l.startsWith(prefix))
                .findFirst()
                .orElseThrow();
        return Integer.parseInt(line.substring(prefix.length()).trim());
    }

    private static void assumeFilesExist(Path... paths) {
        for (Path p : paths) {
            if (!Files.exists(p)) {
                throw new AssertionError("Missing file: " + p.toAbsolutePath());
            }
        }
    }

    private static String normalizeLines(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n").trim();
    }
}
