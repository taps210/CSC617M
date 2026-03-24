package src.ir;

import java.util.List;

/**
 * Three-address IR for a single function (including main).
 * paramNames is used by the interpreter to bind arguments on call (empty for main/update/world).
 * lineTable maps instruction index to source line number (-1 if unavailable).
 */
public record FunctionIR(String name, List<String> paramNames, List<Instr> instructions, List<Integer> lineTable) {
    public FunctionIR(String name, List<Instr> instructions) {
        this(name, List.of(), instructions, List.of());
    }

    public FunctionIR(String name, List<String> paramNames, List<Instr> instructions) {
        this(name, paramNames, instructions, List.of());
    }

    public FunctionIR {
        paramNames = paramNames != null ? List.copyOf(paramNames) : List.of();
        instructions = instructions != null ? List.copyOf(instructions) : List.of();
        lineTable = lineTable != null ? List.copyOf(lineTable) : List.of();
    }
}
