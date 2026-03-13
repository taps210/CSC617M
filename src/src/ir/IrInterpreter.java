package src.ir;

import java.io.*;
import java.util.*;

/**
 * Interprets three-address IR: executes instructions one by one using a store and program counter.
 * Runs main(); function calls push a frame and run callee IR.
 */
public final class IrInterpreter {
    private final Map<String, FunctionIR> functions = new HashMap<>();
    private Map<String, Object> store;
    private FunctionIR currentFunc;
    private int pc;
    private Map<String, Integer> labelMap;
    private final List<Object> paramList = new ArrayList<>();
    private Object returnValue;
    private final InputStream in;
    private final PrintStream out;

    public IrInterpreter(List<FunctionIR> program, InputStream in, PrintStream out) {
        this.in = in != null ? in : System.in;
        this.out = out != null ? out : System.out;
        for (FunctionIR f : program) {
            functions.put(f.name(), f);
        }
    }

    /** Run main. Returns normally or throws on error. */
    public void run() {
        FunctionIR mainFunc = functions.get("main");
        if (mainFunc == null) throw new IllegalStateException("No main function");
        runFunction(mainFunc, List.of());
    }

    private void runFunction(FunctionIR func, List<Object> args) {
        List<Instr> instructions = func.instructions();
        if (instructions.isEmpty()) return;

        Map<String, Object> prevStore = store;
        FunctionIR prevFunc = currentFunc;
        int prevPc = pc;
        Map<String, Integer> prevLabels = labelMap;

        store = new HashMap<>();
        labelMap = buildLabelMap(instructions);
        currentFunc = func;
        pc = 0;

        for (int i = 0; i < func.paramNames().size() && i < args.size(); i++) {
            store.put(func.paramNames().get(i), args.get(i));
        }

        paramList.clear();
        returnValue = null;

        try {
            while (pc >= 0 && pc < instructions.size()) {
                Instr instr = instructions.get(pc);
                boolean advance = execute(instr);
                if (returnValue != null && (instr instanceof Instr.ReturnInstr)) {
                    break;
                }
                if (advance) pc++;
            }
        } finally {
            store = prevStore;
            currentFunc = prevFunc;
            pc = prevPc;
            labelMap = prevLabels;
        }
    }

    private Map<String, Integer> buildLabelMap(List<Instr> instructions) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i) instanceof Instr.LabelInstr l) {
                map.put(l.label(), i);
            }
        }
        return map;
    }

    /** Returns true if PC should advance by 1 (false for jumps/calls that set PC). */
    private boolean execute(Instr instr) {
        if (instr instanceof Instr.LabelInstr) return true;
        if (instr instanceof Instr.AssignConst a) {
            store.put(a.result(), a.value());
            return true;
        }
        if (instr instanceof Instr.AssignCopy a) {
            store.put(a.result(), get(a.source()));
            return true;
        }
        if (instr instanceof Instr.AssignBinary a) {
            Object left = get(a.left());
            Object right = get(a.right());
            store.put(a.result(), evalBinary(left, a.op(), right));
            return true;
        }
        if (instr instanceof Instr.AssignUnary a) {
            Object op = get(a.operand());
            store.put(a.result(), evalUnary(a.op(), op));
            return true;
        }
        if (instr instanceof Instr.GotoInstr g) {
            Integer target = labelMap.get(g.label());
            if (target != null) pc = target;
            return false;
        }
        if (instr instanceof Instr.IfGotoInstr ig) {
            Object cond = get(ig.cond());
            if (truthy(cond)) {
                Integer target = labelMap.get(ig.label());
                if (target != null) pc = target;
                return false;
            }
            return true;
        }
        if (instr instanceof Instr.IfZeroGotoInstr iz) {
            Object cond = get(iz.cond());
            if (!truthy(cond)) {
                Integer target = labelMap.get(iz.label());
                if (target != null) pc = target;
                return false;
            }
            return true;
        }
        if (instr instanceof Instr.ParamInstr p) {
            paramList.add(get(p.arg()));
            return true;
        }
        if (instr instanceof Instr.CallInstr c) {
            FunctionIR callee = functions.get(c.funcName());
            List<Object> args = new ArrayList<>(paramList);
            paramList.clear();
            if (callee != null) {
                runFunction(callee, args);
                Object ret = returnValue;
                returnValue = null;
                if (c.result() != null && ret != null) store.put(c.result(), ret);
            }
            return true;
        }
        if (instr instanceof Instr.ReturnInstr r) {
            returnValue = r.value() != null ? get(r.value()) : null;
            return true;
        }
        if (instr instanceof Instr.ReadInstr r) {
            String name = lvalueName(r.lvalue());
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line = reader.readLine();
                if (line != null) {
                    Object val = parseInput(line);
                    store.put(name, val);
                }
            } catch (IOException e) {
                throw new RuntimeException("Read failed", e);
            }
            return true;
        }
        if (instr instanceof Instr.PrintInstr p) {
            List<Object> vals = new ArrayList<>();
            for (Operand a : p.args()) vals.add(get(a));
            out.println(vals.stream().map(Objects::toString).reduce((a, b) -> a + " " + b).orElse(""));
            return true;
        }
        if (instr instanceof Instr.SpawnInstr) {
            paramList.clear();
            return true;
        }
        if (instr instanceof Instr.MoveInstr) {
            return true;
        }
        if (instr instanceof Instr.StepInstr) {
            return true;
        }
        if (instr instanceof Instr.DestroyInstr) {
            return true;
        }
        if (instr instanceof Instr.NeighborsInstr n) {
            if (n.result() != null) store.put(n.result(), List.of());
            return true;
        }
        if (instr instanceof Instr.AbmCallInstr a) {
            if ("rand".equals(a.name()) && a.args().size() >= 2) {
                Object lo = get(a.args().get(0));
                Object hi = get(a.args().get(1));
                int low = toInt(lo);
                int high = toInt(hi);
                int v = low + (int) (Math.random() * (high - low + 1));
                if (a.result() != null) store.put(a.result(), v);
            } else if (a.result() != null) {
                store.put(a.result(), 0);
            }
            return true;
        }
        return true;
    }

    private Object get(Operand o) {
        if (o instanceof Operand.ConstOperand c) return c.value();
        if (o instanceof Operand.VarOperand v) return store.get(v.name());
        if (o instanceof Operand.TempOperand t) return store.get(t.name());
        return null;
    }

    private String lvalueName(Operand o) {
        if (o instanceof Operand.VarOperand v) return v.name();
        if (o instanceof Operand.TempOperand t) return t.name();
        return "?";
    }

    private static boolean truthy(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return true;
    }

    private static Object evalBinary(Object left, String op, Object right) {
        switch (op) {
            case "+": return add(left, right);
            case "-": return sub(left, right);
            case "*": return mul(left, right);
            case "/": return div(left, right);
            case "%": return mod(left, right);
            case "==": return eq(left, right);
            case "!=": return !eq(left, right);
            case "<": return toInt(left) < toInt(right);
            case "<=": return toInt(left) <= toInt(right);
            case ">": return toInt(left) > toInt(right);
            case ">=": return toInt(left) >= toInt(right);
            case "&&": return truthy(left) && truthy(right);
            case "||": return truthy(left) || truthy(right);
            default: return 0;
        }
    }

    private static Object evalUnary(String op, Object operand) {
        if ("-".equals(op)) return neg(operand);
        if ("!".equals(op)) return !truthy(operand);
        return operand;
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

    private static Object neg(Object o) {
        if (o instanceof Double || o instanceof Float) return -toDouble(o);
        return -toInt(o);
    }

    private static boolean eq(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) return toDouble(a) == toDouble(b);
        return a.equals(b);
    }

    private static Object parseInput(String line) {
        line = line.trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e2) {
                if ("true".equalsIgnoreCase(line)) return true;
                if ("false".equalsIgnoreCase(line)) return false;
                if (line.length() >= 2 && line.startsWith("\"") && line.endsWith("\""))
                    return line.substring(1, line.length() - 1);
                return line;
            }
        }
    }
}
