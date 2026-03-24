package src.ir;

import java.util.Map;

/**
 * A frame in the debug call stack, capturing function context and state at a point in execution.
 */
public record DebugFrame(String functionName, int pc, int sourceLine, Map<String, Object> storeSnapshot) {
}
