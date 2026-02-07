package src;

import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("""
                    Usage:
                      java herd.Main --print <inputFile>
                      java herd.Main --out <outputFile> <inputFile>
                      java herd.Main --bench <inputFile>
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
}
