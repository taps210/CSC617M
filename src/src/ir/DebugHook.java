package src.ir;

import java.util.List;
import java.util.Map;

/**
 * Debug hook interface called by the interpreter before each instruction executes.
 * Implementations can inspect execution state and request pause/step behavior.
 */
public interface DebugHook {
    /**
     * Called before each instruction executes.
     *
     * @param func         the FunctionIR being executed
     * @param pc           program counter (instruction index)
     * @param line         source line number for this instruction (-1 if unavailable)
     * @param store        current variable/temp bindings (unmodifiable)
     * @param callStack    current call stack from oldest to newest frame (unmodifiable)
     * @throws InterruptedException if interrupted (e.g., stop requested)
     */
    void beforeInstruction(FunctionIR func, int pc, int line,
                          Map<String, Object> store,
                          List<DebugFrame> callStack) throws InterruptedException;
}
