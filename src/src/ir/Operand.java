package src.ir;

/**
 * Operand in three-address IR: variable name, temporary, or constant.
 */
public sealed interface Operand {
    /** Variable (identifier from source). */
    record VarOperand(String name) implements Operand {}

    /** Compiler-generated temporary (e.g. t1, t2). */
    record TempOperand(String name) implements Operand {}

    /** Literal constant (Integer, Long, Float, Double, Boolean, String, Character). */
    record ConstOperand(Object value) implements Operand {}

    static Operand var(String name) { return new VarOperand(name); }
    static Operand temp(String name) { return new TempOperand(name); }
    static Operand constant(Object value) { return new ConstOperand(value); }
}
