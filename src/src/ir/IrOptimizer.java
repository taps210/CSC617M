package src.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * IR optimizer pass manager.
 *
 * Optimization #1: Constant Folding
 * - Evaluates compile-time constant expressions in AssignBinary/AssignUnary.
 * - Rewrites to AssignConst when safe.
 * - Includes lightweight constant propagation for variables/temps that were just assigned constants.
 *
 * This pass is local and conservative: it only rewrites arithmetic/logical assignment instructions,
 * and it invalidates tracked constants whenever a symbol is assigned a non-constant value.
 *
 * Optimization #2: Constant Branch Simplification + Unreachable Elimination
 * - Rewrites conditional jumps with compile-time-known conditions:
 *   - if true  -> unconditional goto
 *   - if false -> remove conditional jump
 * - Removes instructions that become unreachable after branch simplification.
 *
 * Optimization #3: Dead Temp Assignment Elimination
 * - Backward liveness pass over temporaries (t*).
 * - Removes pure assignments whose temp result is never used.
 */
public final class IrOptimizer {
    private IrOptimizer() {}

    public record OptimizeResult(
            FunctionIR function,
            int foldedCount,
            int simplifiedBranchCount,
            int removedDeadTempCount
    ) {}

    public static OptimizeResult optimizeFunction(FunctionIR function) {
        if (function == null || function.instructions().isEmpty()) {
            return new OptimizeResult(function, 0, 0, 0);
        }

        FoldResult foldResult = constantFold(function.instructions());
        BranchResult branchResult = simplifyBranchesAndDropUnreachable(foldResult.instructions());
        DeadTempResult deadTempResult = eliminateDeadTempAssignments(branchResult.instructions());

        FunctionIR optimized = new FunctionIR(function.name(), function.paramNames(), deadTempResult.instructions());
        return new OptimizeResult(optimized, foldResult.foldedCount(), branchResult.simplifiedCount(), deadTempResult.removedCount());
    }

    private record FoldResult(List<Instr> instructions, int foldedCount) {}
    private record BranchResult(List<Instr> instructions, int simplifiedCount) {}
    private record DeadTempResult(List<Instr> instructions, int removedCount) {}

    // -------- Optimization #1 --------
    private static FoldResult constantFold(List<Instr> instructions) {
        Map<String, Object> knownConstants = new HashMap<>();
        List<Instr> out = new ArrayList<>(instructions.size());
        int folded = 0;

        for (Instr ins : instructions) {
            // Control-flow joins/splits break linear constant propagation assumptions.
            if (ins instanceof Instr.LabelInstr) {
                knownConstants.clear();
                out.add(ins);
                continue;
            }

            if (ins instanceof Instr.AssignConst a) {
                knownConstants.put(a.result(), a.value());
                out.add(ins);
                continue;
            }

            if (ins instanceof Instr.AssignCopy a) {
                Object srcConst = resolveConst(a.source(), knownConstants);
                if (srcConst != null) {
                    out.add(new Instr.AssignConst(a.result(), srcConst));
                    knownConstants.put(a.result(), srcConst);
                    folded++;
                } else {
                    out.add(ins);
                    knownConstants.remove(a.result());
                }
                continue;
            }

            if (ins instanceof Instr.AssignUnary a) {
                Object operandConst = resolveConst(a.operand(), knownConstants);
                if (operandConst != null) {
                    Object result = evalUnary(a.op(), operandConst);
                    out.add(new Instr.AssignConst(a.result(), result));
                    knownConstants.put(a.result(), result);
                    folded++;
                } else {
                    out.add(ins);
                    knownConstants.remove(a.result());
                }
                continue;
            }

            if (ins instanceof Instr.AssignBinary a) {
                Object leftConst = resolveConst(a.left(), knownConstants);
                Object rightConst = resolveConst(a.right(), knownConstants);
                if (leftConst != null && rightConst != null) {
                    Object result = evalBinary(leftConst, a.op(), rightConst);
                    out.add(new Instr.AssignConst(a.result(), result));
                    knownConstants.put(a.result(), result);
                    folded++;
                } else {
                    out.add(ins);
                    knownConstants.remove(a.result());
                }
                continue;
            }

            if (ins instanceof Instr.IfGotoInstr || ins instanceof Instr.IfZeroGotoInstr || ins instanceof Instr.GotoInstr) {
                out.add(ins);
                knownConstants.clear();
                continue;
            }

            // If an instruction writes to a tracked symbol, drop stale constant info.
            invalidateWrittenSymbols(ins, knownConstants);
            out.add(ins);
        }

        return new FoldResult(out, folded);
    }

    // -------- Optimization #2 --------
    private static BranchResult simplifyBranchesAndDropUnreachable(List<Instr> instructions) {
        Map<String, Object> knownConstants = new HashMap<>();
        List<Instr> simplified = new ArrayList<>(instructions.size());
        int simplifiedCount = 0;

        for (Instr ins : instructions) {
            // Control-flow joins/splits invalidate linear constant facts.
            // This keeps branch simplification conservative and loop-safe.
            if (ins instanceof Instr.LabelInstr) {
                knownConstants.clear();
                simplified.add(ins);
                continue;
            }

            if (ins instanceof Instr.AssignConst a) {
                knownConstants.put(a.result(), a.value());
                simplified.add(ins);
                continue;
            }
            if (ins instanceof Instr.AssignCopy a) {
                Object c = resolveConst(a.source(), knownConstants);
                if (c != null) knownConstants.put(a.result(), c);
                else knownConstants.remove(a.result());
                simplified.add(ins);
                continue;
            }
            if (ins instanceof Instr.AssignUnary a) {
                knownConstants.remove(a.result());
                simplified.add(ins);
                continue;
            }
            if (ins instanceof Instr.AssignBinary a) {
                knownConstants.remove(a.result());
                simplified.add(ins);
                continue;
            }

            if (ins instanceof Instr.IfGotoInstr ig) {
                Object c = resolveConst(ig.cond(), knownConstants);
                if (c != null) {
                    if (truthy(c)) {
                        simplified.add(new Instr.GotoInstr(ig.label()));
                    }
                    simplifiedCount++;
                } else {
                    simplified.add(ins);
                }
                knownConstants.clear();
                continue;
            }

            if (ins instanceof Instr.IfZeroGotoInstr iz) {
                Object c = resolveConst(iz.cond(), knownConstants);
                if (c != null) {
                    if (!truthy(c)) {
                        simplified.add(new Instr.GotoInstr(iz.label()));
                    }
                    simplifiedCount++;
                } else {
                    simplified.add(ins);
                }
                knownConstants.clear();
                continue;
            }

            if (ins instanceof Instr.GotoInstr) {
                simplified.add(ins);
                knownConstants.clear();
                continue;
            }

            invalidateWrittenSymbols(ins, knownConstants);
            simplified.add(ins);
        }

        List<Instr> reachableOnly = dropUnreachableInstructions(simplified);
        return new BranchResult(reachableOnly, simplifiedCount);
    }

    private static List<Instr> dropUnreachableInstructions(List<Instr> instructions) {
        if (instructions.isEmpty()) return instructions;
        Map<String, Integer> labels = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i) instanceof Instr.LabelInstr l) {
                labels.put(l.label(), i);
            }
        }

        Set<Integer> reachable = new HashSet<>();
        markReachable(0, instructions, labels, reachable);
        List<Instr> out = new ArrayList<>(reachable.size());
        for (int i = 0; i < instructions.size(); i++) {
            if (reachable.contains(i)) out.add(instructions.get(i));
        }
        return out;
    }

    private static void markReachable(int start,
                                      List<Instr> instructions,
                                      Map<String, Integer> labels,
                                      Set<Integer> reachable) {
        int i = start;
        while (i >= 0 && i < instructions.size()) {
            if (!reachable.add(i)) return;
            Instr ins = instructions.get(i);

            if (ins instanceof Instr.GotoInstr g) {
                Integer target = labels.get(g.label());
                if (target != null) markReachable(target, instructions, labels, reachable);
                return;
            }
            if (ins instanceof Instr.IfGotoInstr g) {
                Integer target = labels.get(g.label());
                if (target != null) markReachable(target, instructions, labels, reachable);
                i++;
                continue;
            }
            if (ins instanceof Instr.IfZeroGotoInstr g) {
                Integer target = labels.get(g.label());
                if (target != null) markReachable(target, instructions, labels, reachable);
                i++;
                continue;
            }
            if (ins instanceof Instr.ReturnInstr) {
                return;
            }
            i++;
        }
    }

    // -------- Optimization #3 --------
    private static DeadTempResult eliminateDeadTempAssignments(List<Instr> instructions) {
        Set<String> live = new HashSet<>();
        List<Instr> keptReversed = new ArrayList<>(instructions.size());
        int removed = 0;

        for (int i = instructions.size() - 1; i >= 0; i--) {
            Instr ins = instructions.get(i);
            String def = definedSymbol(ins);

            if (def != null && isTempName(def) && isPureTempDef(ins) && !live.contains(def)) {
                removed++;
                continue;
            }

            keptReversed.add(ins);
            if (def != null) live.remove(def);
            live.addAll(usedSymbols(ins));
        }

        Collections.reverse(keptReversed);
        return new DeadTempResult(keptReversed, removed);
    }

    private static String definedSymbol(Instr ins) {
        if (ins instanceof Instr.AssignConst a) return a.result();
        if (ins instanceof Instr.AssignCopy a) return a.result();
        if (ins instanceof Instr.AssignUnary a) return a.result();
        if (ins instanceof Instr.AssignBinary a) return a.result();
        if (ins instanceof Instr.AllocArrayInstr a) return a.result();
        if (ins instanceof Instr.CallInstr c) return c.result();
        if (ins instanceof Instr.AbmCallInstr a) return a.result();
        if (ins instanceof Instr.NeighborsInstr n) return n.result();
        if (ins instanceof Instr.AgentMethodCallInstr a) return a.result();
        return null;
    }

    private static boolean isPureTempDef(Instr ins) {
        return ins instanceof Instr.AssignConst
                || ins instanceof Instr.AssignCopy
                || ins instanceof Instr.AssignUnary
                || ins instanceof Instr.AssignBinary;
    }

    private static boolean isTempName(String name) {
        return name != null && name.startsWith("t");
    }

    private static Set<String> usedSymbols(Instr ins) {
        Set<String> used = new HashSet<>();
        if (ins instanceof Instr.AssignCopy a) addOperandSymbol(used, a.source());
        else if (ins instanceof Instr.AssignUnary a) addOperandSymbol(used, a.operand());
        else if (ins instanceof Instr.AssignBinary a) {
            addOperandSymbol(used, a.left());
            addOperandSymbol(used, a.right());
        } else if (ins instanceof Instr.IfGotoInstr a) addOperandSymbol(used, a.cond());
        else if (ins instanceof Instr.IfZeroGotoInstr a) addOperandSymbol(used, a.cond());
        else if (ins instanceof Instr.ParamInstr a) addOperandSymbol(used, a.arg());
        else if (ins instanceof Instr.ReturnInstr a && a.value() != null) addOperandSymbol(used, a.value());
        else if (ins instanceof Instr.ReadInstr a) addOperandSymbol(used, a.lvalue());
        else if (ins instanceof Instr.PrintInstr a) for (Operand op : a.args()) addOperandSymbol(used, op);
        else if (ins instanceof Instr.ArrayStoreInstr a) {
            used.add(a.arrayName());
            addOperandSymbol(used, a.index());
            addOperandSymbol(used, a.value());
        } else if (ins instanceof Instr.SpawnInstr a) for (Operand op : a.args()) addOperandSymbol(used, op);
        else if (ins instanceof Instr.MoveInstr a) {
            addOperandSymbol(used, a.x());
            addOperandSymbol(used, a.y());
            if (a.z() != null) addOperandSymbol(used, a.z());
        } else if (ins instanceof Instr.StepInstr a && a.arg() != null) addOperandSymbol(used, a.arg());
        else if (ins instanceof Instr.DestroyInstr a) addOperandSymbol(used, a.target());
        else if (ins instanceof Instr.NeighborsInstr a) for (Operand op : a.args()) addOperandSymbol(used, op);
        else if (ins instanceof Instr.AbmCallInstr a) for (Operand op : a.args()) addOperandSymbol(used, op);
        else if (ins instanceof Instr.ZoneEnterInstr a) addOperandSymbol(used, a.radius());
        else if (ins instanceof Instr.AgentMethodCallInstr a) used.add(a.handle());
        return used;
    }

    private static void addOperandSymbol(Set<String> used, Operand op) {
        if (op instanceof Operand.VarOperand v) used.add(v.name());
        else if (op instanceof Operand.TempOperand t) used.add(t.name());
    }

    private static Object resolveConst(Operand op, Map<String, Object> known) {
        if (op instanceof Operand.ConstOperand c) return c.value();
        if (op instanceof Operand.VarOperand v) return known.get(v.name());
        if (op instanceof Operand.TempOperand t) return known.get(t.name());
        return null;
    }

    private static void invalidateWrittenSymbols(Instr ins, Map<String, Object> known) {
        if (ins instanceof Instr.AllocArrayInstr a) {
            known.remove(a.result());
            return;
        }
        if (ins instanceof Instr.CallInstr c && c.result() != null) {
            known.remove(c.result());
            return;
        }
        if (ins instanceof Instr.AbmCallInstr a && a.result() != null) {
            known.remove(a.result());
            return;
        }
        if (ins instanceof Instr.NeighborsInstr n && n.result() != null) {
            known.remove(n.result());
        }
    }

    private static Object evalUnary(String op, Object operand) {
        return switch (op) {
            case "-" -> (operand instanceof Double || operand instanceof Float) ? -toDouble(operand) : -toInt(operand);
            case "!" -> !truthy(operand);
            case "+" -> operand;
            default -> operand;
        };
    }

    private static Object evalBinary(Object left, String op, Object right) {
        return switch (op) {
            case "+" -> add(left, right);
            case "-" -> sub(left, right);
            case "*" -> mul(left, right);
            case "/" -> div(left, right);
            case "%" -> mod(left, right);
            case "==" -> eq(left, right);
            case "!=" -> !eq(left, right);
            case "<" -> toInt(left) < toInt(right);
            case "<=" -> toInt(left) <= toInt(right);
            case ">" -> toInt(left) > toInt(right);
            case ">=" -> toInt(left) >= toInt(right);
            case "&&" -> truthy(left) && truthy(right);
            case "||" -> truthy(left) || truthy(right);
            default -> 0;
        };
    }

    private static boolean truthy(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return true;
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Object add(Object a, Object b) {
        if (a instanceof String || b instanceof String) return String.valueOf(a) + b;
        if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float)
            return toDouble(a) + toDouble(b);
        return toInt(a) + toInt(b);
    }

    private static Object sub(Object a, Object b) {
        if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float)
            return toDouble(a) - toDouble(b);
        return toInt(a) - toInt(b);
    }

    private static Object mul(Object a, Object b) {
        if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float)
            return toDouble(a) * toDouble(b);
        return toInt(a) * toInt(b);
    }

    private static Object div(Object a, Object b) {
        if (a instanceof Double || b instanceof Double || a instanceof Float || b instanceof Float)
            return toDouble(a) / toDouble(b);
        int r = toInt(b);
        return r == 0 ? 0 : toInt(a) / r;
    }

    private static Object mod(Object a, Object b) {
        int r = toInt(b);
        return r == 0 ? 0 : toInt(a) % r;
    }

    private static boolean eq(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) return toDouble(a) == toDouble(b);
        return a.equals(b);
    }
}
