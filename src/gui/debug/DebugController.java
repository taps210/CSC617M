package src.gui.debug;

import src.Ast;
import src.gui.core.BreakpointListener;
import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.core.RuntimeEventListener;
import src.gui.editor.EditorPanel;
import src.ir.DebugFrame;
import src.ir.DebugHook;
import src.ir.FunctionIR;
import src.ir.IrInterpreter;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;

/**
 * Core debugger controller implementing debug hook, breakpoint listener, and compile/runtime listeners.
 * Manages pause/resume via Semaphore and step controls (over/into/out).
 */
public class DebugController implements DebugHook, BreakpointListener, CompileListener, RuntimeEventListener {
    private final EditorPanel editorPanel;
    private final DebuggerPanel debuggerPanel;

    // Cached compilation state
    private List<FunctionIR> cachedIrFuncs;
    private Ast.ProgramNode cachedAst;

    // Breakpoint tracking
    private volatile Set<Integer> breakpoints = new CopyOnWriteArraySet<>();

    // Pause/resume state
    private final Semaphore pauseSemaphore = new Semaphore(0);
    private volatile DebugCommand nextCommand = DebugCommand.NONE;
    private volatile int stepOverDepth = -1;
    private volatile int stepOutTargetDepth = -1;

    // Current execution state (updated by beforeInstruction)
    private volatile int currentCallDepth = 0;

    // Debug thread reference
    private Thread debugThread;

    public DebugController(EditorPanel editorPanel, DebuggerPanel debuggerPanel) {
        this.editorPanel = editorPanel;
        this.debuggerPanel = debuggerPanel;
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        // Caching is now handled by CompileController
        // This callback is mainly here for the listener interface
    }

    @Override
    public void onBreakpointsChanged(Set<Integer> lines) {
        this.breakpoints = new CopyOnWriteArraySet<>(lines);
    }

    @Override
    public void onRuntimeStarted() {
        // Debug session started
    }

    @Override
    public void onRuntimeOutput(String output) {
        // Delegate to debugger panel if needed
    }

    @Override
    public void onRuntimeFinished(String finalOutput, String runtimeError) {
        SwingUtilities.invokeLater(() -> {
            debuggerPanel.onDebugSessionEnded();
            editorPanel.clearDebugHighlight();
        });
    }

    @Override
    public void beforeInstruction(FunctionIR func, int pc, int line,
                                  Map<String, Object> store,
                                  List<DebugFrame> callStack) throws InterruptedException {
        currentCallDepth = callStack.size();

        // Determine if we should pause at this instruction
        boolean shouldPause = breakpoints.contains(line)
                || (nextCommand == DebugCommand.STEP_OVER && currentCallDepth <= stepOverDepth)
                || (nextCommand == DebugCommand.STEP_INTO)
                || (nextCommand == DebugCommand.STEP_OUT && currentCallDepth < stepOutTargetDepth);

        if (shouldPause) {
            // Notify UI (on EDT via SwingUtilities.invokeLater)
            SwingUtilities.invokeLater(() ->
                debuggerPanel.onDebugPause(line, store, callStack));

            // Block interpreter thread until user steps/continues
            try {
                pauseSemaphore.acquire();
            } catch (InterruptedException e) {
                throw new InterruptedException("Debugger pause interrupted");
            }
        }
    }

    // Step control methods (called from UI thread)
    public void stepOver() {
        stepOverDepth = currentCallDepth;
        nextCommand = DebugCommand.STEP_OVER;
        pauseSemaphore.release();
    }

    public void stepInto() {
        nextCommand = DebugCommand.STEP_INTO;
        pauseSemaphore.release();
    }

    public void stepOut() {
        stepOutTargetDepth = currentCallDepth - 1;
        nextCommand = DebugCommand.STEP_OUT;
        pauseSemaphore.release();
    }

    public void resume() {
        nextCommand = DebugCommand.CONTINUE;
        pauseSemaphore.release();
    }

    public void stopDebugSession() {
        if (debugThread != null && debugThread.isAlive()) {
            debugThread.interrupt();
        }
    }

    /**
     * Start a debug session with the current compiled IR.
     * Called when F5 is pressed.
     */
    public void startDebugSession(List<FunctionIR> irFuncs, Ast.ProgramNode ast) {
        if (irFuncs == null || ast == null) {
            JOptionPane.showMessageDialog(null, "Please compile first.");
            return;
        }

        // Reset state for new session
        nextCommand = DebugCommand.NONE;
        stepOverDepth = -1;
        stepOutTargetDepth = -1;
        currentCallDepth = 0;

        // Create interpreter with this debugger as the hook
        IrInterpreter interpreter = new IrInterpreter(irFuncs, ast, System.in, System.out, this);

        // Start on dedicated thread
        debugThread = new Thread(() -> {
            try {
                interpreter.run();
            } catch (Exception e) {
                onRuntimeFinished("", e.getMessage());
            } finally {
                onRuntimeFinished("", null);
            }
        }, "herd-debug");

        debugThread.start();
    }

    private enum DebugCommand {
        NONE, STEP_OVER, STEP_INTO, STEP_OUT, CONTINUE
    }
}
