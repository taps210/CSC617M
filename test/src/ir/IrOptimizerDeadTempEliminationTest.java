package src.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IrOptimizerDeadTempEliminationTest {

    @Test
    void removesTempAssignmentsThatDoNotAffectProgramOutput() {
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.AssignConst("t1", 1),
                        new Instr.AssignConst("t2", 2),
                        new Instr.AssignBinary("t3", Operand.temp("t1"), "+", Operand.temp("t2")),
                        new Instr.PrintInstr(List.of(Operand.constant(0)))
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        assertTrue(opt.removedDeadTempCount() >= 1);
        assertTrue(ins.stream().noneMatch(i -> i instanceof Instr.AssignConst a && a.result().startsWith("t")));
        assertTrue(ins.stream().noneMatch(i -> i instanceof Instr.AssignBinary a && a.result().startsWith("t")));
        assertEquals(1, ins.stream().filter(i -> i instanceof Instr.PrintInstr).count());
    }

    @Test
    void keepsTempAssignmentsWhenValueIsUsedLater() {
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.AssignConst("t1", 4),
                        new Instr.AssignConst("t2", 5),
                        new Instr.AssignBinary("t3", Operand.temp("t1"), "+", Operand.temp("t2")),
                        new Instr.PrintInstr(List.of(Operand.temp("t3")))
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        assertTrue(ins.stream().anyMatch(i -> i instanceof Instr.AssignConst a && "t3".equals(a.result())));
        assertTrue(ins.stream().anyMatch(i -> i instanceof Instr.PrintInstr));
    }
}
