package src.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that exposes the AgentMethodCallInstr handle tracking bug.
 *
 * The bug: AgentMethodCallInstr uses a 'handle' variable (String),
 * but usedSymbols() doesn't track it. This causes the handle variable's
 * assignment to be incorrectly eliminated as dead.
 */
class IrOptimizerAgentMethodBugTest {

    @Test
    void agentMethodHandleNotTrackedAsUsed() {
        // Scenario: t1 is assigned, then used as a handle in AgentMethodCallInstr
        // Without the fix, t1 assignment would be incorrectly eliminated as dead
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.AssignConst("t1", 42),           // Define t1 (pure assignment)
                        new Instr.AgentMethodCallInstr("t1", "getAge", "t2"),  // Use t1 as handle
                        new Instr.PrintInstr(List.of(Operand.temp("t2")))       // Use t2
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        // The assignment to t1 should NOT be eliminated, even though it looks dead
        // (the dead temp eliminator doesn't see that AgentMethodCallInstr uses t1)
        assertTrue(
            ins.stream().anyMatch(i -> i instanceof Instr.AssignConst a && "t1".equals(a.result())),
            "t1 assignment was incorrectly eliminated - AgentMethodCallInstr.handle is not tracked as used!"
        );
    }

    @Test
    void agentMethodCallPreservedWhenHandleIsUsed() {
        FunctionIR f = new FunctionIR(
                "main",
                List.of(),
                List.of(
                        new Instr.AssignConst("agent", 123),       // Create agent reference
                        new Instr.AgentMethodCallInstr("agent", "doSomething", "result"),
                        new Instr.PrintInstr(List.of(Operand.temp("result")))
                )
        );

        IrOptimizer.OptimizeResult opt = IrOptimizer.optimizeFunction(f);
        List<Instr> ins = opt.function().instructions();

        // agent assignment should be kept (it's used by AgentMethodCallInstr)
        assertTrue(
            ins.stream().anyMatch(i -> i instanceof Instr.AssignConst a && "agent".equals(a.result())),
            "agent variable should be preserved for use in AgentMethodCallInstr"
        );

        // AgentMethodCallInstr itself must be kept
        assertTrue(
            ins.stream().anyMatch(i -> i instanceof Instr.AgentMethodCallInstr),
            "AgentMethodCallInstr should never be removed"
        );
    }
}
