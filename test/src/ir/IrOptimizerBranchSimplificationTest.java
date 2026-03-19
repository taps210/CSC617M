package src.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IrOptimizerBranchSimplificationTest {

    @Test
    void simplifiesIfZeroWithConstantAndRemovesUnreachableFallthrough() {
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.AssignConst("t1", 0),
                        new Instr.IfZeroGotoInstr(Operand.temp("t1"), "L1"),
                        new Instr.PrintInstr(List.of(Operand.constant(111))),
                        new Instr.LabelInstr("L1"),
                        new Instr.PrintInstr(List.of(Operand.constant(222)))
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        assertTrue(opt.simplifiedBranchCount() >= 1);
        assertTrue(ins.stream().noneMatch(i -> i instanceof Instr.IfZeroGotoInstr));
        assertTrue(ins.stream().anyMatch(i -> i instanceof Instr.GotoInstr));
        assertTrue(ins.stream().noneMatch(i -> i instanceof Instr.PrintInstr p
                && !p.args().isEmpty()
                && p.args().get(0) instanceof Operand.ConstOperand c
                && Integer.valueOf(111).equals(c.value())));
    }

    @Test
    void removesAlwaysFalseIfBranch() {
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.AssignConst("t1", false),
                        new Instr.IfGotoInstr(Operand.temp("t1"), "L1"),
                        new Instr.PrintInstr(List.of(Operand.constant(1))),
                        new Instr.LabelInstr("L1"),
                        new Instr.ReturnInstr(null)
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        assertTrue(opt.simplifiedBranchCount() >= 1);
        assertTrue(ins.stream().noneMatch(i -> i instanceof Instr.IfGotoInstr));
    }
}
