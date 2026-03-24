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
import src.ir.IrInterpreter;
import src.ir.IrOptimizer;
import src.semantic.SemanticAnalyzer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
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
                        java -cp build/classes/java/main src.Main --ir --no-opt <inputFile>
                        java -cp build/classes/java/main src.Main --ir --opt-trace <inputFile>
                        java -cp build/classes/java/main src.Main --run <inputFile>
                        java -cp build/classes/java/main src.Main --run --out <outputFile> <inputFile>
                        java -cp build/classes/java/main src.Main --cfg <inputFile>
                        java -cp build/classes/java/main src.Main --cfg --out <outputFile> <inputFile>
                        java -cp build/classes/java/main src.Main --bench <inputFile>
                    """);
            return;
        }

        String mode = args[0];
        int i = 1;
        String outFile = null;
        boolean noOpt = false;
        boolean optTrace = false;

        while (i < args.length - 1 && args[i].startsWith("--")) {
            switch (args[i]) {
                case "--out" -> {
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
                case "--no-opt" -> {
                    noOpt = true;
                    i++;
                }
                case "--opt-trace" -> {
                    optTrace = true;
                    i++;
                }
                default -> {
                    // Unknown flag; stop parsing options and treat the rest as positional.
                    i = args.length - 1;
                }
            }
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
            case "--ir" -> runIr(inFile, outFile, noOpt, optTrace);
            case "--run" -> runRun(inFile, outFile);
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
            var errors = new ArrayList<LexicalErrorRecord>();
            List<Token> tokens = scanner.tokenizeAll(errors);
            for (var t : tokens) sb.append(t).append(System.lineSeparator());
            for (var e : errors) sb.append(e.line()).append(":").append(e.col()).append(" ").append(e.message()).append(System.lineSeparator());
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
                Token t = scanner.nextToken();
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
        var sb = new StringBuilder();
        try {
            List<Token> tokens = new Scanner(srcText).tokenizeAll();
            new Parser(tokens, sb).parseProgram();
            sb.append("Parse OK").append(System.lineSeparator());
        } catch (LexicalErrorRecord.ScanAbortedException | ParseException e) {
            sb.append(e.getMessage()).append(System.lineSeparator());
        }
        String result = sb.toString();
        if (outputFile != null) {
            Files.writeString(Path.of(outputFile), result);
            System.out.println("Wrote parse dump to: " + outputFile);
        } else {
            System.out.print(result);
        }
    }

    // ---------------- SEMANTIC MODE ----------------

    private static void runSemantic(String inputFile, String outputFile) throws Exception {
        var sb = new StringBuilder();
        try {
            List<Token> tokens = new Scanner(Files.readString(Path.of(inputFile))).tokenizeAll();
            ProgramNode ast = new Parser(tokens, new StringBuilder()).parseProgramToAst();
            List<SemanticError> errors = SemanticAnalyzer.analyze(ast);
            if (errors.isEmpty()) {
                sb.append("Semantic OK").append(System.lineSeparator());
            } else {
                for (var e : errors)
                    sb.append(e.line()).append(":").append(e.col()).append(" ").append(e.message()).append(System.lineSeparator());
            }
        } catch (LexicalErrorRecord.ScanAbortedException | ParseException e) {
            sb.append(e.getMessage()).append(System.lineSeparator());
        }
        writeResult(sb.toString(), outputFile, "semantic result");
    }

    // ---------------- IR MODE ----------------

    private static void runIr(String inputFile, String outputFile, boolean noOpt, boolean optTrace) throws Exception {
        var sb = new StringBuilder();
        ProgramNode ast = parseAndAnalyze(inputFile, sb);
        if (ast != null) {
            try {
                List<FunctionIR> funcs = IrBuilder.buildProgram(ast);
                if (optTrace) {
                    for (FunctionIR f : funcs) {
                        IrOptimizer.OptimizeTrace trace = IrOptimizer.optimizeFunctionWithTrace(f);
                        sb.append("==== IR TRACE: ").append(f.name()).append(" ====").append(System.lineSeparator());
                        sb.append("-- RAW --").append(System.lineSeparator());
                        sb.append(IrFormatter.formatFunctionIR(trace.original())).append(System.lineSeparator());

                        sb.append("-- AFTER #1 Constant Folding (folded ").append(trace.foldedCount()).append(") --").append(System.lineSeparator());
                        sb.append(IrFormatter.formatFunctionIR(trace.afterConstantFolding())).append(System.lineSeparator());

                        sb.append("-- AFTER #2 Branch Simplification + Unreachable (simplified ").append(trace.simplifiedBranchCount()).append(") --").append(System.lineSeparator());
                        sb.append(IrFormatter.formatFunctionIR(trace.afterBranchSimplification())).append(System.lineSeparator());

                        sb.append("-- AFTER #3 Dead Temp Elimination (removed ").append(trace.removedDeadTempCount()).append(") --").append(System.lineSeparator());
                        sb.append(IrFormatter.formatFunctionIR(trace.afterDeadTempElimination())).append(System.lineSeparator());
                    }
                } else if (noOpt) {
                    for (FunctionIR f : funcs)
                        sb.append(IrFormatter.formatFunctionIR(f)).append(System.lineSeparator());
                } else {
                    for (FunctionIR f : optimizeAll(funcs))
                        sb.append(IrFormatter.formatFunctionIR(f)).append(System.lineSeparator());
                }
            } catch (Exception e) {
                sb.append("IR build failed: ").append(e.getMessage()).append(System.lineSeparator());
            }
        }
        writeResult(sb.toString(), outputFile, "IR");
    }

    // ---------------- RUN (INTERPRET) MODE ----------------

    private static void runRun(String inputFile, String outputFile) throws Exception {
        var sb = new StringBuilder();
        ProgramNode ast = parseAndAnalyze(inputFile, sb);
        if (ast == null) {
            writeResult(sb.toString(), outputFile, "interpreter output");
            return;
        }
        List<FunctionIR> funcs;
        try {
            funcs = optimizeAll(IrBuilder.buildProgram(ast));
        } catch (Exception e) {
            sb.append("IR build failed: ").append(e.getMessage()).append(System.lineSeparator());
            writeResult(sb.toString(), outputFile, "interpreter output");
            return;
        }
        if (outputFile != null) {
            // Capture to file: buffer first, then write when done
            var buffer = new ByteArrayOutputStream();
            var ps = new PrintStream(buffer);
            try {
                new IrInterpreter(funcs, ast, System.in, ps).run();
            } catch (Exception e) {
                sb.append("Runtime error: ").append(e.getMessage()).append(System.lineSeparator());
                writeResult(sb.toString(), outputFile, "interpreter output");
                return;
            }
            ps.flush();
            writeResult(buffer.toString(), outputFile, "interpreter output");
        } else {
            // No output file: stream directly to stdout so output appears in real time
            try {
                new IrInterpreter(funcs, ast, System.in, System.out).run();
            } catch (Exception e) {
                System.out.println("Runtime error: " + e.getMessage());
            }
        }
    }

    // ---------------- CFG MODE ----------------

    private static void runCfg(String inputFile, String outputFile) throws Exception {
        var sb = new StringBuilder();
        ProgramNode ast = parseAndAnalyze(inputFile, sb);
        if (ast != null) {
            try {
                for (FunctionIR f : optimizeAll(IrBuilder.buildProgram(ast))) {
                    sb.append("function ").append(f.name()).append(System.lineSeparator());
                    List<BasicBlocks.Block> blocks = BasicBlocks.build(f.instructions());
                    sb.append(IrFormatter.formatCFG(new ControlFlowGraph(blocks))).append(System.lineSeparator());
                }
            } catch (Exception e) {
                sb.append("IR build failed: ").append(e.getMessage()).append(System.lineSeparator());
            }
        }
        writeResult(sb.toString(), outputFile, "CFG");
    }

    /** Returns AST if scan/parse/semantic succeed; appends errors to sb and returns null otherwise. */
    private static ProgramNode parseAndAnalyze(String inputFile, StringBuilder sb) throws Exception {
        List<Token> tokens;
        try {
            tokens = new Scanner(readSourceWithFallback(inputFile)).tokenizeAll();
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            sb.append(e.getMessage()).append(System.lineSeparator());
            return null;
        }
        ProgramNode ast;
        try {
            ast = new Parser(tokens, new StringBuilder()).parseProgramToAst();
        } catch (ParseException e) {
            sb.append(e.getMessage()).append(System.lineSeparator());
            return null;
        }
        List<SemanticError> errors = SemanticAnalyzer.analyze(ast);
        if (!errors.isEmpty()) {
            for (var e : errors)
                sb.append(e.line()).append(":").append(e.col()).append(" ").append(e.message()).append(System.lineSeparator());
            return null;
        }
        return ast;
    }

    /**
     * Reads a source file, with helpful fallbacks when the user provides only a basename.
     * This keeps the CLI friendly during live demos.
     */
    private static String readSourceWithFallback(String inputFile) throws Exception {
        Path p = Path.of(inputFile);
        try {
            return Files.readString(p);
        } catch (NoSuchFileException first) {
            // If user passed a relative basename, try common project folders.
            if (!p.isAbsolute()) {
                List<Path> fallbacks = Arrays.asList(
                        Path.of("tests", "rubric", inputFile),
                        Path.of("tests", inputFile),
                        Path.of("examples", inputFile)
                );
                for (Path fp : fallbacks) {
                    try {
                        return Files.readString(fp);
                    } catch (NoSuchFileException ignored) {
                        // keep trying
                    }
                }
            }
            throw first;
        }
    }

    /** Writes result to outputFile if given, otherwise prints to stdout. */
    private static void writeResult(String result, String outputFile, String label) throws Exception {
        if (outputFile != null) {
            Files.writeString(Path.of(outputFile), result);
            System.out.println("Wrote " + label + " to: " + outputFile);
        } else {
            System.out.print(result);
        }
    }

    private static List<FunctionIR> optimizeAll(List<FunctionIR> funcs) {
        List<FunctionIR> out = new ArrayList<>(funcs.size());
        for (FunctionIR f : funcs) {
            out.add(IrOptimizer.optimizeFunction(f).function());
        }
        return out;
    }
}