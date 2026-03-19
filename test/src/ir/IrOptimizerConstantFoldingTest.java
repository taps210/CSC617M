package src.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IrOptimizerConstantFoldingTest {

    @Test
    void foldsConstantArithmeticChain() {
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.AssignConst("t1", 2),
                        new Instr.AssignConst("t2", 3),
                        new Instr.AssignBinary("t3", Operand.temp("t1"), "+", Operand.temp("t2")),
                        new Instr.AssignBinary("t4", Operand.temp("t3"), "*", Operand.constant(10)),
                        new Instr.PrintInstr(List.of(Operand.temp("t4")))
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        assertTrue(opt.foldedCount() >= 2);
        assertTrue(ins.stream().anyMatch(i -> i instanceof Instr.AssignConst a
                && "t4".equals(a.result())
                && Integer.valueOf(50).equals(a.value())));
        assertTrue(ins.stream().anyMatch(i -> i instanceof Instr.PrintInstr));
    }

    @Test
    void foldsUnaryAndBooleanExpressions() {
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.AssignConst("a", 7),
                        new Instr.AssignUnary("n", "-", Operand.var("a")),
                        new Instr.AssignConst("b", true),
                        new Instr.AssignUnary("nb", "!", Operand.var("b")),
                        new Instr.AssignBinary("ok", Operand.var("nb"), "||", Operand.constant(true)),
                        new Instr.PrintInstr(List.of(Operand.var("n"), Operand.var("ok")))
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        Object negValue = ins.stream()
                .filter(i -> i instanceof Instr.AssignConst a && "n".equals(a.result()))
                .map(i -> ((Instr.AssignConst) i).value())
                .findFirst()
                .orElseThrow();
        assertTrue(negValue instanceof Number);
        assertEquals(-7.0, ((Number) negValue).doubleValue(), 0.000001);
        assertTrue(ins.stream().anyMatch(i -> i instanceof Instr.AssignConst a
                && "ok".equals(a.result())
                && Boolean.TRUE.equals(a.value())));
    }

    @Test
    void doesNotFoldWhenOperandsAreNotCompileTimeConstants() {
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.ReadInstr(Operand.var("x")),
                        new Instr.AssignBinary("t", Operand.var("x"), "+", Operand.constant(1)),
                        new Instr.PrintInstr(List.of(Operand.var("t")))
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        assertEquals(0, opt.foldedCount());
        assertTrue(ins.get(1) instanceof Instr.AssignBinary);
    }
}
