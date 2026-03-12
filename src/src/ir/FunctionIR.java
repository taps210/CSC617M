package src.ir;

import java.util.List;

/**
 * Three-address IR for a single function (including main).
 */
public record FunctionIR(String name, List<Instr> instructions) {
    public FunctionIR {
        instructions = instructions != null ? List.copyOf(instructions) : List.of();
    }
}
