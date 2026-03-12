package src;

import src.errors.LexicalErrorRecord;
import src.errors.ParseException;
import src.Ast.ProgramNode;
import src.errors.SemanticError;
import src.ir.BasicBlocks;
import src.ir.ControlFlowGraph;
import src.ir.FunctionIR;
import src.ir.IrBuilder;
import src.ir.IrFormatter;
import src.semantic.SemanticAnalyzer;

import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("""
                    Usage:
                        java -cp build/classes/java/main src.Main --scan <inputFile>
                        java -cp build/classes/java/main src.Main --scan --out <outputFile> <inputFile>
                        java -cp build/classes/java/main src.Main --parse <inputFile>
                        java -cp build/classes/java/main src.Main --parse --out <outputFile> <inputFile>
                        java -cp build/classes/java/main src.Main --semantic <inputFile>
                        java -cp build/classes/java/main src.Main --semantic --out <outputFile> <inputFile>
                        java -cp build/classes/java/main src.Main --ir <inputFile>
                        java -cp build/classes/java/main src.Main --ir --out <outputFile> <inputFile>
                        java -cp build/classes/java/main src.Main --cfg <inputFile>
                        java -cp build/classes/java/main src.Main --cfg --out <outputFile> <inputFile>
                        java -cp build/classes/java/main src.Main --bench <inputFile>
                    """);
            return;
        }

        String mode = args[0];
        int i = 1;
        String outFile = null;
        if (i < args.length && args[i].equals("--out")) {
            if (mode.equals("--bench")) {
                System.out.println("--bench does not use --out.");
                return;
            }
            i++;
            if (i >= args.length) {
                System.out.println("Missing output file after --out.");
                return;
            }
            outFile = args[i++];
        }
        if (i >= args.length) {
            System.out.println("Missing input file.");
            return;
        }
        String inFile = args[i];

        switch (mode) {
            case "--scan" -> runScan(inFile, outFile);
            case "--parse" -> runParse(inFile, outFile);
            case "--semantic" -> runSemantic(inFile, outFile);
            case "--ir" -> runIr(inFile, outFile);
            case "--cfg" -> runCfg(inFile, outFile);
            case "--bench" -> runBench(inFile);
            default -> System.out.println("Unknown option: " + mode);
        }
    }

    // ---------------- SCANNER MODE ----------------

    private static void runScan(String inputFile, String outputFile) throws Exception {
        String srcText = Files.readString(Path.of(inputFile));
        var scanner = new Scanner(srcText);

        var sb = new StringBuilder();
        try {
            List<Token> tokens = scanner.tokenizeAll(true);
            for (var t : tokens) sb.append(t).append(System.lineSeparator());
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            sb.append(e.getMessage()).append(System.lineSeparator());
        }

        String result = sb.toString();
        if (outputFile != null) {
            Files.writeString(Path.of(outputFile), result);
            System.out.println("Wrote token dump to: " + outputFile);
        } else {
            System.out.print(result);
        }
    }

    private static void runBench(String inputFile) throws Exception {
        String srcText = Files.readString(Path.of(inputFile));
        var scanner = new Scanner(srcText);

        Instant t0 = Instant.now();
        int count = 0;
        try {
            while (true) {
                Token t = scanner.nextToken(false);
                count++;
                if (t.type() == TokenType.EOF) break;
            }
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            // for bench, just stop on error
        }
        Instant t1 = Instant.now();

        System.out.println("Tokens: " + count);
        System.out.println("Elapsed: " + Duration.between(t0, t1).toMillis() + " ms");
    }

    // ---------------- PARSER MODE ----------------

    private static void runParse(String inputFile, String outputFile) throws Exception {
        String srcText = Files.readString(Path.of(inputFile));
        var scanner = new Scanner(srcText);

        Appendable out = outputFile != null ? new StringBuilder() : System.out;
        try {
            List<Token> tokens = scanner.tokenizeAll(false);
            var parser = new Parser(tokens, out);
            parser.parseProgram();
            if (outputFile != null) {
                ((StringBuilder) out).append("Parse OK").append(System.lineSeparator());
                Files.writeString(Path.of(outputFile), out.toString());
                System.out.println("Wrote parse dump to: " + outputFile);
            } else {
                System.out.println("Parse OK");
            }
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            String msg = e.getMessage();
            if (outputFile != null) {
                Files.writeString(Path.of(outputFile), msg + System.lineSeparator());
                System.out.println("Wrote parse dump to: " + outputFile);
            } else {
                System.out.println(msg);
            }
        } catch (ParseException e) {
            String msg = e.getMessage();
            if (outputFile != null) {
                Files.writeString(Path.of(outputFile), msg + System.lineSeparator());
                System.out.println("Wrote parse dump to: " + outputFile);
            } else {
                System.out.println(msg);
            }
        }
    }

    // ---------------- SEMANTIC MODE ----------------

    private static void runSemantic(String inputFile, String outputFile) throws Exception {
        String srcText = Files.readString(Path.of(inputFile));
        var scanner = new Scanner(srcText);

        List<Token> tokens;
        try {
            tokens = scanner.tokenizeAll(false);
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            String msg = e.getMessage();
            if (outputFile != null) Files.writeString(Path.of(outputFile), msg + System.lineSeparator());
            else System.out.println(msg);
            return;
        }

        ProgramNode ast;
        try {
            var parser = new Parser(tokens, new StringBuilder());
            ast = parser.parseProgramToAst();
        } catch (ParseException e) {
            String msg = e.getMessage();
            if (outputFile != null) Files.writeString(Path.of(outputFile), msg + System.lineSeparator());
            else System.out.println(msg);
            return;
        }

        List<SemanticError> errors = new SemanticAnalyzer().analyze(ast);
        String result;
        if (errors.isEmpty()) {
            result = "Semantic OK" + System.lineSeparator();
        } else {
            result = errors.stream()
                    .map(e -> e.line() + ":" + e.col() + " " + e.message())
                    .reduce("", (a, b) -> a + b + System.lineSeparator());
        }

        if (outputFile != null) {
            Files.writeString(Path.of(outputFile), result);
            System.out.println("Wrote semantic result to: " + outputFile);
        } else {
            System.out.print(result);
        }
    }

    // ---------------- IR MODE ----------------

    private static void runIr(String inputFile, String outputFile) throws Exception {
        ProgramNode ast = parseAndAnalyze(inputFile, outputFile);
        if (ast == null) return;
        List<FunctionIR> funcs;
        try {
            funcs = IrBuilder.buildProgram(ast);
        } catch (Exception e) {
            String msg = "IR build failed: " + e.getMessage();
            if (outputFile != null) {
                Files.writeString(Path.of(outputFile), msg + System.lineSeparator());
                System.out.println("Wrote error to: " + outputFile);
            } else {
                System.err.println(msg);
                e.printStackTrace(System.err);
            }
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (FunctionIR f : funcs) {
            sb.append(IrFormatter.formatFunctionIR(f)).append("\n");
        }
        String result = sb.toString();
        if (outputFile != null) {
            Files.writeString(Path.of(outputFile), result);
            System.out.println("Wrote IR to: " + outputFile);
        } else {
            System.out.print(result);
        }
    }

    // ---------------- CFG MODE ----------------

    private static void runCfg(String inputFile, String outputFile) throws Exception {
        ProgramNode ast = parseAndAnalyze(inputFile, outputFile);
        if (ast == null) return;
        List<FunctionIR> funcs;
        try {
            funcs = IrBuilder.buildProgram(ast);
        } catch (Exception e) {
            String msg = "IR build failed: " + e.getMessage();
            if (outputFile != null) {
                Files.writeString(Path.of(outputFile), msg + System.lineSeparator());
                System.out.println("Wrote error to: " + outputFile);
            } else {
                System.err.println(msg);
                e.printStackTrace(System.err);
            }
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (FunctionIR f : funcs) {
            sb.append("function ").append(f.name()).append("\n");
            List<BasicBlocks.Block> blocks = BasicBlocks.build(f.instructions());
            ControlFlowGraph cfg = new ControlFlowGraph(blocks);
            sb.append(IrFormatter.formatCFG(cfg)).append("\n");
        }
        String result = sb.toString();
        if (outputFile != null) {
            Files.writeString(Path.of(outputFile), result);
            System.out.println("Wrote CFG to: " + outputFile);
        } else {
            System.out.print(result);
        }
    }

    /** Returns AST if scan/parse/semantic succeed; prints errors and returns null otherwise. */
    private static ProgramNode parseAndAnalyze(String inputFile, String outputFile) throws Exception {
        String srcText = Files.readString(Path.of(inputFile));
        List<Token> tokens;
        try {
            tokens = new Scanner(srcText).tokenizeAll(false);
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            String msg = e.getMessage();
            if (outputFile != null) Files.writeString(Path.of(outputFile), msg + System.lineSeparator());
            else System.out.println(msg);
            return null;
        }
        ProgramNode ast;
        try {
            var parser = new Parser(tokens, new StringBuilder());
            ast = parser.parseProgramToAst();
        } catch (ParseException e) {
            String msg = e.getMessage();
            if (outputFile != null) Files.writeString(Path.of(outputFile), msg + System.lineSeparator());
            else System.out.println(msg);
            return null;
        }
        List<SemanticError> errors = new SemanticAnalyzer().analyze(ast);
        if (!errors.isEmpty()) {
            String result = errors.stream()
                    .map(e -> e.line() + ":" + e.col() + " " + e.message())
                    .reduce("", (a, b) -> a + b + System.lineSeparator());
            if (outputFile != null) {
                Files.writeString(Path.of(outputFile), result);
                System.out.println("Wrote errors to: " + outputFile);
            } else {
                System.out.print(result);
            }
            return null;
        }
        return ast;
    }
}