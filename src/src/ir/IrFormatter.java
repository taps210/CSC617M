package src.ir;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formats three-address IR and CFG for human-readable output.
 */
public final class IrFormatter {

    public static String formatOperand(Operand o) {
        if (o instanceof Operand.VarOperand v) return v.name();
        if (o instanceof Operand.TempOperand t) return t.name();
        if (o instanceof Operand.ConstOperand c) return formatConst(c.value());
        return "?";
    }

    private static String formatConst(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + ((String) value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (value instanceof Character) return "'" + value + "'";
        return value.toString();
    }

    public static String formatInstr(Instr i) {
        if (i instanceof Instr.LabelInstr l) return l.label() + ":";
        if (i instanceof Instr.AssignBinary a) return a.result() + " = " + formatOperand(a.left()) + " " + a.op() + " " + formatOperand(a.right());
        if (i instanceof Instr.AssignUnary a) return a.result() + " = " + a.op() + " " + formatOperand(a.operand());
        if (i instanceof Instr.AssignCopy a) return a.result() + " = " + formatOperand(a.source());
        if (i instanceof Instr.AssignConst a) return a.result() + " = " + formatConst(a.value());
        if (i instanceof Instr.GotoInstr g) return "goto " + g.label();
        if (i instanceof Instr.IfGotoInstr ig) return "if " + formatOperand(ig.cond()) + " goto " + ig.label();
        if (i instanceof Instr.IfZeroGotoInstr iz) return "if " + formatOperand(iz.cond()) + " == 0 goto " + iz.label();
        if (i instanceof Instr.ParamInstr p) return "param " + formatOperand(p.arg());
        if (i instanceof Instr.CallInstr c) return (c.result() != null ? c.result() + " = " : "") + "call " + c.funcName();
        if (i instanceof Instr.ReturnInstr r) return r.value() != null ? "return " + formatOperand(r.value()) : "return";
        if (i instanceof Instr.ReadInstr r) return "read " + formatOperand(r.lvalue());
        if (i instanceof Instr.PrintInstr p) return "print " + p.args().stream().map(IrFormatter::formatOperand).collect(Collectors.joining(", "));
        if (i instanceof Instr.AllocArrayInstr a) return a.result() + " = new array[" + a.size() + "]";
        if (i instanceof Instr.ArrayStoreInstr a) return a.arrayName() + "[" + formatOperand(a.index()) + "] = " + formatOperand(a.value());
        if (i instanceof Instr.SpawnInstr s) return "spawn " + s.agentType() + "(" + s.args().stream().map(IrFormatter::formatOperand).collect(Collectors.joining(", ")) + ")";
        if (i instanceof Instr.MoveInstr m) return "move(" + formatOperand(m.x()) + ", " + formatOperand(m.y()) + ", " + (m.z() != null ? formatOperand(m.z()) : "null") + ")";
        if (i instanceof Instr.StepInstr s) return "step(" + (s.arg() != null ? formatOperand(s.arg()) : "") + ")";
        if (i instanceof Instr.DestroyInstr d) return "destroy(" + formatOperand(d.target()) + ")";
        if (i instanceof Instr.NeighborsInstr n) return (n.result() != null ? n.result() + " = " : "") + "neighbors(" + n.args().stream().map(IrFormatter::formatOperand).collect(Collectors.joining(", ")) + ")";
        if (i instanceof Instr.AbmCallInstr a) return (a.result() != null ? a.result() + " = " : "") + "abmcall " + a.name() + "(" + a.args().stream().map(IrFormatter::formatOperand).collect(Collectors.joining(", ")) + ")";
        if (i instanceof Instr.ZoneEnterInstr z) return "zone_enter " + z.targetType() + " radius=" + formatOperand(z.radius()) + " skip=" + z.skipLabel();
        return "?";
    }

    public static String formatFunctionIR(FunctionIR f) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("function ").append(f.name()).append(nl);
        for (Instr i : f.instructions()) {
            sb.append("  ").append(formatInstr(i)).append(nl);
        }
        return sb.toString();
    }

    public static String formatCFG(ControlFlowGraph cfg) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cfg.blockCount(); i++) {
            BasicBlocks.Block b = cfg.blocks().get(i);
            sb.append("  block ").append(i).append(" (").append(b.label()).append("):").append(nl);
            for (Instr in : b.instructions()) {
                sb.append("    ").append(formatInstr(in)).append(nl);
            }
            Set<Integer> succ = cfg.successors(i);
            if (!succ.isEmpty()) {
                sb.append("    -> ").append(succ.stream().sorted().map(j -> "B" + j).collect(Collectors.joining(", "))).append(nl);
            }
        }
        return sb.toString();
    }
}
