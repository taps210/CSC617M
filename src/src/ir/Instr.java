package src.ir;

import java.util.List;

/**
 * Three-address IR instructions. Each instruction has at most one operator.
 */
public sealed interface Instr
        permits Instr.LabelInstr, Instr.AssignBinary, Instr.AssignUnary, Instr.AssignCopy, Instr.AssignConst,
        Instr.GotoInstr, Instr.IfGotoInstr, Instr.IfZeroGotoInstr, Instr.ParamInstr, Instr.CallInstr, Instr.ReturnInstr,
        Instr.ReadInstr, Instr.PrintInstr,
        Instr.SpawnInstr, Instr.MoveInstr, Instr.StepInstr, Instr.DestroyInstr, Instr.NeighborsInstr, Instr.AbmCallInstr {

    /** Label (target for goto / if-goto). */
    record LabelInstr(String label) implements Instr {}

    /** result = left op right (binary arithmetic, comparison). */
    record AssignBinary(String result, Operand left, String op, Operand right) implements Instr {}

    /** result = op operand (unary: -, !). */
    record AssignUnary(String result, String op, Operand operand) implements Instr {}

    /** result = source (copy). */
    record AssignCopy(String result, Operand source) implements Instr {}

    /** result = constant. */
    record AssignConst(String result, Object value) implements Instr {}

    /** goto label. */
    record GotoInstr(String label) implements Instr {}

    /** if cond != 0 goto label. */
    record IfGotoInstr(Operand cond, String label) implements Instr {}

    /** if cond == 0 goto label. */
    record IfZeroGotoInstr(Operand cond, String label) implements Instr {}

    /** pass argument to next call. */
    record ParamInstr(Operand arg) implements Instr {}

    /** result = call funcName(args); result is null for void. */
    record CallInstr(String funcName, String result) implements Instr {}

    /** return [value]; value is null for void. */
    record ReturnInstr(Operand value) implements Instr {}

    /** read into lvalue. */
    record ReadInstr(Operand lvalue) implements Instr {}

    /** print(args). */
    record PrintInstr(List<Operand> args) implements Instr {}

    /** spawn agentType(args). */
    record SpawnInstr(String agentType, List<Operand> args) implements Instr {}

    /** move(x, y, z); z may be null for 2D. */
    record MoveInstr(Operand x, Operand y, Operand z) implements Instr {}

    /** step(arg); arg may be null. */
    record StepInstr(Operand arg) implements Instr {}

    /** destroy(target). */
    record DestroyInstr(Operand target) implements Instr {}

    /** result = neighbors(args); returns collection of nearby agents. result is null for statement form. */
    record NeighborsInstr(List<Operand> args, String result) implements Instr {}

    /** ABM call (e.g. rand): name(args); result is null for statement form. */
    record AbmCallInstr(String name, List<Operand> args, String result) implements Instr {}
}
