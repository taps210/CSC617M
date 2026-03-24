package src.gui.core;

import src.errors.LexicalErrorRecord;
import src.errors.ParseException;
import src.errors.SemanticError;
import src.Parser;
import src.Scanner;
import src.Token;
import static src.Ast.*;
import src.gui.model.CompileError;
import src.parsetree.ParseTreeNode;
import src.gui.model.CompileMetrics;
import src.semantic.SemanticAnalyzer;
import src.semantic.SemanticResult;
import src.semantic.SymbolEntry;
import src.ir.IrBuilder;
import src.ir.IrFormatter;
import src.ir.FunctionIR;
import src.ir.IrInterpreter;
import src.ir.BasicBlocks;
import src.ir.ControlFlowGraph;
import src.ir.IrOptimizer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.SwingUtilities;

/**
 * Single compile authority. Runs scanner and parser, measures timing,
 * builds CompileResult and CompileMetrics, notifies listeners.
 */
public class CompileController {
    private static final class InteractiveRuntimeSession {
        final PrintStream inputWriter;
        final StringBuilder output = new StringBuilder();
        final Thread thread;

        InteractiveRuntimeSession(PrintStream inputWriter, Thread thread) {
            this.inputWriter = inputWriter;
            this.thread = thread;
        }
    }

    private CompileResult lastResult;
    private List<FunctionIR> lastIrFuncs;
    private ProgramNode lastAst;
    private final List<CompileListener> listeners = new ArrayList<>();
    private String runtimeInput = "";
    private RuntimeEventListener runtimeEventListener;
    private InteractiveRuntimeSession runtimeSession;
    private final List<String> pendingInputLines = new ArrayList<>();
    private volatile boolean compileInProgress;
    private volatile boolean runtimeStopRequested;
    private volatile boolean runtimeStopNotified;

    public void addListener(CompileListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CompileListener listener) {
        listeners.remove(listener);
    }

    public CompileResult getLastResult() {
        return lastResult;
    }

    public List<FunctionIR> getLastIrFuncs() {
        return lastIrFuncs;
    }

    public ProgramNode getLastAst() {
        return lastAst;
    }

    public synchronized void setRuntimeEventListener(RuntimeEventListener runtimeEventListener) {
        this.runtimeEventListener = runtimeEventListener;
    }

    public synchronized boolean isRuntimeActive() {
        return runtimeSession != null && runtimeSession.thread.isAlive();
    }

    public synchronized void stopRuntime() {
        runtimeStopRequested = true;
        if (runtimeSession == null) {
            return;
        }
        String partialOutput = runtimeSession.output.toString();
        try {
            runtimeSession.inputWriter.close();
        } catch (Exception ignored) {
        }
        runtimeSession.thread.interrupt();
        runtimeSession = null;
        pendingInputLines.clear();
        runtimeStopRequested = false;
        runtimeStopNotified = true;
        RuntimeEventListener listener = runtimeEventListener;
        if (listener != null) listener.onRuntimeFinished(partialOutput, "Stopped by user");
    }

    public boolean isCompiling() {
        return compileInProgress;
    }

    public synchronized void submitRuntimeInputLine(String line) {
        String value = line != null ? line : "";
        if (isRuntimeActive()) {
            runtimeSession.inputWriter.print(value);
            runtimeSession.inputWriter.print(System.lineSeparator());
            runtimeSession.inputWriter.flush();
        } else {
            pendingInputLines.add(value);
        }
    }

    public void setRuntimeInput(String runtimeInput) {
        this.runtimeInput = runtimeInput != null ? runtimeInput : "";
    }

    public void compileInteractive(String sourceText) {
        compileInternal(sourceText, true);
    }

    public void compile(String sourceText) {
        compileInternal(sourceText, false);
    }

    private void compileInternal(String sourceText, boolean interactiveRuntime) {
        compileInProgress = true;
        try {
        stopRuntimeSession();
        CompileMetrics metrics = new CompileMetrics();
        computeSourceMetrics(sourceText, metrics);

        List<CompileError> allErrors = new ArrayList<>();
        List<Token> tokens = new ArrayList<>();

        // Scan
        long t0 = System.nanoTime();
        List<LexicalErrorRecord> lexErrors = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(sourceText);
            tokens = scanner.tokenizeAll(lexErrors);
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            // error already added to lexErrors by tokenizeAll before it rethrew
        }
        long t1 = System.nanoTime();
        metrics.scanTimeNs = t1 - t0;
        metrics.totalTokens = tokens.size();
        metrics.scanErrorCount = lexErrors.size();

        for (LexicalErrorRecord r : lexErrors) {
            allErrors.add(new CompileError(r.line(), r.col(), r.message(), CompileError.Source.LEXER, CompileError.Severity.ERROR));
        }

        String parserTrace = "";
        Optional<ProgramNode> ast = Optional.empty();
        Optional<ParseTreeNode> parseTree = Optional.empty();
        List<SymbolEntry> symbolEntries = List.of();
        if (!tokens.isEmpty()) {
            StringBuilder trace = new StringBuilder();
            long p0 = System.nanoTime();
            Parser parser = new Parser(tokens, trace);
            try {
                parser.parseProgram();
                trace.append("Parse OK").append(System.lineSeparator());
                ast = Optional.ofNullable(parser.getProgramNode());
                parseTree = Optional.ofNullable(parser.getParseTreeRoot());
            } catch (ParseException e) {
                allErrors.add(new CompileError(e.line(), e.col(), e.getMessage(), CompileError.Source.PARSER, CompileError.Severity.ERROR));
                trace.append("Parse error: ").append(e.getMessage()).append(System.lineSeparator());
            }
            long p1 = System.nanoTime();
            metrics.parseTimeNs = p1 - p0;
            metrics.parseNodeCount = parser.getConstructCount();
            metrics.parseErrorCount = (int) allErrors.stream().filter(err -> err.source() == CompileError.Source.PARSER).count();
            parserTrace = trace.toString();

            // Semantic analysis after successful parse when AST is present
            if (ast.isPresent()) {
                SemanticResult semResult = SemanticAnalyzer.analyzeDetailed(ast.get());
                for (SemanticError se : semResult.errors()) {
                    allErrors.add(se.toCompileError());
                }
                symbolEntries = semResult.symbols();
            }
        }

        metrics.parseWarningCount = 0; // v1: no warnings from parser

        // IR generation (only when AST present and no errors)
        Optional<String> irText = Optional.empty();
        Optional<String> cfgText = Optional.empty();
        Optional<String> interpreterOutput = Optional.empty();
        List<FunctionIR> irFuncs = List.of();
        boolean hasErrors = allErrors.stream().anyMatch(e -> e.severity() == CompileError.Severity.ERROR);
        if (ast.isPresent() && !hasErrors) {
            long ir0 = System.nanoTime();
            try {
                irFuncs = IrBuilder.buildProgram(ast.get());
                // Optimization #1: constant folding at IR level.
                List<FunctionIR> optimized = new ArrayList<>(irFuncs.size());
                for (FunctionIR f : irFuncs) {
                    optimized.add(IrOptimizer.optimizeFunction(f).function());
                }
                irFuncs = optimized;
                // Cache IR and AST for debugger
                lastIrFuncs = irFuncs;
                lastAst = ast.get();
                StringBuilder irSb = new StringBuilder();
                StringBuilder cfgSb = new StringBuilder();
                for (FunctionIR f : irFuncs) {
                    irSb.append(IrFormatter.formatFunctionIR(f)).append(System.lineSeparator());
                    metrics.irInstrCount += f.instructions().size();
                    List<BasicBlocks.Block> blocks = BasicBlocks.build(f.instructions());
                    cfgSb.append(IrFormatter.formatCFG(new ControlFlowGraph(blocks))).append(System.lineSeparator());
                }
                irText = Optional.of(irSb.toString());
                cfgText = Optional.of(cfgSb.toString());
            } catch (Exception e) {
                allErrors.add(new CompileError(0, 0, e.getMessage(), CompileError.Source.IR, CompileError.Severity.ERROR));
                lastIrFuncs = null;
                lastAst = null;
            }
            metrics.irTimeNs = System.nanoTime() - ir0;
        }

        // Interpretation (only when IR was built successfully)
        if (irText.isPresent()) {
            if (interactiveRuntime) {
                interpreterOutput = Optional.of("");
            } else {
                long run0 = System.nanoTime();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream capturedOut = new PrintStream(baos);
                try {
                    byte[] inputBytes = runtimeInput.getBytes(StandardCharsets.UTF_8);
                    new IrInterpreter(irFuncs, ast.get(), new ByteArrayInputStream(inputBytes), capturedOut).run();
                } catch (Exception e) {
                    allErrors.add(new CompileError(0, 0, e.getMessage(), CompileError.Source.RUNTIME, CompileError.Severity.ERROR));
                }
                metrics.runTimeNs = System.nanoTime() - run0;
                interpreterOutput = Optional.of(baos.toString());
            }
        }

        CompileResult result = new CompileResult(sourceText, tokens, parserTrace, allErrors, metrics, ast, parseTree,
                irText, cfgText, interpreterOutput, symbolEntries);
        this.lastResult = result;
        notifyListeners(result);

        if (interactiveRuntime && ast.isPresent() && !hasErrors) {
            if (runtimeStopRequested) {
                runtimeStopRequested = false;
                RuntimeEventListener listener = runtimeEventListener;
                if (listener != null) listener.onRuntimeFinished("", "Stopped by user");
            } else {
            startInteractiveRuntime(irFuncs, ast.get());
            }
        }
        } finally {
            compileInProgress = false;
        }
    }

    private void startInteractiveRuntime(List<FunctionIR> functions, ProgramNode ast) {
        try {
            runtimeStopRequested = false;
            java.io.PipedInputStream inPipe = new java.io.PipedInputStream();
            java.io.PipedOutputStream outPipe = new java.io.PipedOutputStream(inPipe);
            PrintStream inputWriter = new PrintStream(outPipe, true, StandardCharsets.UTF_8);

            OutputStream runtimeOut = new OutputStream() {
                @Override
                public void write(int b) {
                    write(new byte[]{(byte) b}, 0, 1);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    if (len <= 0) return;
                    String text = new String(b, off, len, StandardCharsets.UTF_8);
                    synchronized (CompileController.this) {
                        if (runtimeSession != null) runtimeSession.output.append(text);
                    }
                    RuntimeEventListener listener = runtimeEventListener;
                    if (listener != null) listener.onRuntimeOutput(text);
                }
            };

            Thread runtimeThread = new Thread(() -> {
                RuntimeEventListener listener = runtimeEventListener;
                if (listener != null) listener.onRuntimeStarted();
                String runtimeError = null;
                try {
                    PrintStream liveOut = new PrintStream(runtimeOut, true, StandardCharsets.UTF_8);
                    new IrInterpreter(functions, ast, inPipe, liveOut).run();
                } catch (Exception ex) {
                    runtimeError = ex.getMessage();
                } finally {
                    String finalOutput;
                    boolean stopped;
                    boolean stopNotified;
                    synchronized (CompileController.this) {
                        finalOutput = runtimeSession != null ? runtimeSession.output.toString() : "";
                        stopped = runtimeStopRequested;
                        stopNotified = runtimeStopNotified;
                        runtimeSession = null;
                        runtimeStopRequested = false;
                        runtimeStopNotified = false;
                    }
                    RuntimeEventListener endListener = runtimeEventListener;
                    if (endListener != null && !stopNotified) {
                        endListener.onRuntimeFinished(finalOutput, stopped ? "Stopped by user" : runtimeError);
                    }
                }
            }, "herd-runtime");

            synchronized (this) {
                runtimeSession = new InteractiveRuntimeSession(inputWriter, runtimeThread);
            }

            for (String line : drainPendingInputLines()) {
                submitRuntimeInputLine(line);
            }
            runtimeThread.start();
        } catch (Exception e) {
            RuntimeEventListener listener = runtimeEventListener;
            if (listener != null) listener.onRuntimeFinished("", e.getMessage());
        }
    }

    private synchronized List<String> drainPendingInputLines() {
        List<String> copy = new ArrayList<>(pendingInputLines);
        pendingInputLines.clear();
        return copy;
    }

    private synchronized void stopRuntimeSession() {
        if (runtimeSession == null) return;
        try {
            runtimeSession.inputWriter.close();
        } catch (Exception ignored) {
        }
        runtimeSession.thread.interrupt();
        runtimeSession = null;
        pendingInputLines.clear();
    }

    private void notifyListeners(CompileResult result) {
        if (SwingUtilities.isEventDispatchThread()) {
            for (CompileListener l : listeners) {
                l.onCompileComplete(result);
            }
            return;
        }
        SwingUtilities.invokeLater(() -> {
            for (CompileListener l : listeners) {
                l.onCompileComplete(result);
            }
        });
    }

    private void computeSourceMetrics(String sourceText, CompileMetrics metrics) {
        if (sourceText == null || sourceText.isEmpty()) {
            metrics.totalLines = 0;
            metrics.codeLines = 0;
            metrics.blankLines = 0;
            metrics.commentLines = 0;
            metrics.charCount = 0;
            metrics.wordCount = 0;
            return;
        }
        metrics.charCount = sourceText.length();
        String[] words = sourceText.split("\\s+", -1);
        int wordCount = 0;
        for (String w : words) {
            if (!w.isEmpty()) wordCount++;
        }
        metrics.wordCount = wordCount;
        String[] lines = sourceText.split("\\r?\\n", -1);
        metrics.totalLines = lines.length;
        int blank = 0;
        int comment = 0;
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) blank++;
            else if (t.startsWith("//")) comment++;
            // block comment lines not tracked for simplicity
        }
        metrics.blankLines = blank;
        metrics.commentLines = comment;
        metrics.codeLines = metrics.totalLines - blank - comment;
    }

}
