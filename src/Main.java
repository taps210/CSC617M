package src;

import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("""
                    Usage:
                      java src.Main --print <inputFile>
                      java src.Main --out <outputFile> <inputFile>
                      java src.Main --bench <inputFile>
                      java src.Main --collect-errors <inputFile> [outputFile]
                    """);
            return;
        }

        switch (args[0]) {
            case "--print" -> runPrint(args[1]);
            case "--out" -> {
                if (args.length < 3) {
                    System.out.println("Missing output or input file.");
                    return;
                }
                runFile(args[1], args[2]);
            }
            case "--bench" -> runBench(args[1]);
            case "--collect-errors" -> {
                String outFile = args.length >= 3 ? args[2] : null;
                runCollectErrors(args[1], outFile);
            }
            default -> System.out.println("Unknown option: " + args[0]);
        }
    }

    private static void runPrint(String inputFile) throws Exception {
        String src = Files.readString(Path.of(inputFile));
        var scanner = new Scanner(src);

        try {
            List<Token> tokens = scanner.tokenizeAll(true);
            tokens.forEach(System.out::println);
        } catch (LexicalException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void runFile(String outputFile, String inputFile) throws Exception {
        String src = Files.readString(Path.of(inputFile));
        var scanner = new Scanner(src);

        var sb = new StringBuilder();
        try {
            List<Token> tokens = scanner.tokenizeAll(true);
            for (var t : tokens) sb.append(t).append(System.lineSeparator());
        } catch (LexicalException e) {
            sb.append(e.getMessage()).append(System.lineSeparator());
        }

        Files.writeString(Path.of(outputFile), sb.toString());
        System.out.println("Wrote token dump to: " + outputFile);
    }

    private static void runBench(String inputFile) throws Exception {
        String src = Files.readString(Path.of(inputFile));
        var scanner = new Scanner(src);

        Instant t0 = Instant.now();
        int count = 0;
        try {
            while (true) {
                Token t = scanner.nextToken(false);
                count++;
                if (t.type() == TokenType.EOF) break;
            }
        } catch (LexicalException e) {
            // for bench, just stop on error
        }
        Instant t1 = Instant.now();

        System.out.println("Tokens: " + count);
        System.out.println("Elapsed: " + Duration.between(t0, t1).toMillis() + " ms");
    }

    private static void runCollectErrors(String inputFile, String outputFile) throws Exception {
        String src = Files.readString(Path.of(inputFile));
        var scanner = new Scanner(src);
        var errors = new ArrayList<LexicalErrorRecord>();

        try {
            scanner.tokenizeAll(false, errors);
        } catch (LexicalException e) {
            // Non-recoverable error (e.g. unterminated string); add it and stop
            errors.add(new LexicalErrorRecord(e.line, e.col, e.getMessage().contains("\n")
                    ? e.getMessage().substring(e.getMessage().indexOf('\n') + 1).trim()
                    : e.getMessage()));
        }

        var lines = errors.stream().map(LexicalErrorRecord::format).toList();
        String result = String.join(System.lineSeparator(), lines);

        if (outputFile != null) {
            Files.writeString(Path.of(outputFile), result);
            System.out.println("Wrote " + errors.size() + " error(s) to: " + outputFile);
        } else {
            lines.forEach(System.out::println);
        }
    }
}
