package src.ir;

import java.util.List;

/**
 * Three-address IR for a single function (including main).
 * paramNames is used by the interpreter to bind arguments on call (empty for main/update/world).
 */
public record FunctionIR(String name, List<String> paramNames, List<Instr> instructions) {
    public FunctionIR(String name, List<Instr> instructions) {
        this(name, List.of(), instructions != null ? List.copyOf(instructions) : List.of());
    }

    public FunctionIR {
        paramNames = paramNames != null ? List.copyOf(paramNames) : List.of();
        instructions = instructions != null ? List.copyOf(instructions) : List.of();
    }
}
