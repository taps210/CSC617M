package src.ir;

import src.Ast;

import java.io.*;
import java.util.*;

/**
 * Interprets three-address IR: executes instructions one by one using a store and program counter.
 * Runs main(); function calls push a frame and run callee IR.
 * When ProgramNode is provided, ABM instructions (spawn, move, step, destroy, neighbors) use a minimal runtime.
 */
public final class IrInterpreter {
    private final Map<String, FunctionIR> functions = new HashMap<>();
    private final Ast.ProgramNode program;
    private Map<String, Object> store;
    private FunctionIR currentFunc;
    private int pc;
    private Map<String, Integer> labelMap;
    private final List<Object> paramList = new ArrayList<>();
    private Object returnValue;
    private final InputStream in;
    private final PrintStream out;

    /** ABM runtime state (used when program != null). */
    private final List<AgentHandle> agents = new ArrayList<>();
    private final Map<String, Object> worldStore = new HashMap<>();
    private AgentHandle currentAgent;
    private String worldName;
    private int nextAgentId;

    /** Identifies an agent for destroy/neighbors. Stored in agent list and in lists returned by neighbors(). */
    public static final class AgentHandle {
        public final int id;
        public final String typeName;
        public final Map<String, Object> store;

        AgentHandle(int id, String typeName, Map<String, Object> store) {
            this.id = id;
            this.typeName = typeName;
            this.store = store;
        }
    }

    public IrInterpreter(List<FunctionIR> program, InputStream in, PrintStream out) {
        this(program, null, in, out);
    }

    public IrInterpreter(List<FunctionIR> funcs, Ast.ProgramNode program, InputStream in, PrintStream out) {
        this.program = program;
        this.in = in != null ? in : System.in;
        this.out = out != null ? out : System.out;
        for (FunctionIR f : funcs) {
            functions.put(f.name(), f);
        }
        if (program != null) {
            Ast.WorldDeclNode world = findFirstWorld(program);
            if (world != null) {
                worldName = world.name();
                initWorldStore(world);
            }
        }
    }

    private static Ast.WorldDeclNode findFirstWorld(Ast.ProgramNode p) {
        for (Ast.TypeDeclNode td : p.typeDecls()) {
            if (td instanceof Ast.WorldDeclNode w) return w;
        }
        return null;
    }

    private void initWorldStore(Ast.WorldDeclNode world) {
        for (Ast.VarDeclNode v : world.fields()) {
            for (Ast.DeclaratorNode d : v.declarators()) {
                Object init = d.init() != null ? evalLiteralInit(d.init()) : defaultForType(v.dataType().baseTypeName());
                worldStore.put(d.name(), init);
            }
        }
    }

    private static Object evalLiteralInit(Ast.ExprNode e) {
        if (e instanceof Ast.LiteralExprNode n) return n.value();
        return 0;
    }

    private static Object defaultForType(String typeName) {
        return switch (typeName.toLowerCase()) {
            case "bool", "boolean" -> false;
            case "float", "double" -> 0.0;
            default -> 0;
        };
    }

    /** Run main. Returns normally or throws on error. */
    public void run() {
        FunctionIR mainFunc = functions.get("main");
        if (mainFunc == null) throw new IllegalStateException("No main function");
        runFunction(mainFunc, List.of());
    }

    private void runFunction(FunctionIR func, List<Object> args) {
        runFunction(func, args, null);
    }

    /** If initialStore != null, use it (for world_* / update_*); otherwise create fresh store and bind params. */
    private void runFunction(FunctionIR func, List<Object> args, Map<String, Object> initialStore) {
        List<Instr> instructions = func.instructions();
        if (instructions.isEmpty()) return;

        Map<String, Object> prevStore = store;
        FunctionIR prevFunc = currentFunc;
        int prevPc = pc;
        Map<String, Integer> prevLabels = labelMap;
        AgentHandle prevAgent = currentAgent;

        store = initialStore != null ? initialStore : new HashMap<>();
        if (initialStore == null) {
            for (int i = 0; i < func.paramNames().size() && i < args.size(); i++) {
                store.put(func.paramNames().get(i), args.get(i));
            }
        }
        labelMap = buildLabelMap(instructions);
        currentFunc = func;
        pc = 0;
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
            currentAgent = prevAgent;
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
            Object resultVal;
            if ("[]".equals(a.op())) {
                resultVal = listIndex(left, right);
            } else if (".".equals(a.op()) && a.right() instanceof Operand.VarOperand vr && "length".equals(vr.name()) && left instanceof List<?> list) {
                resultVal = list.size();
            } else {
                resultVal = evalBinary(left, a.op(), right);
            }
            store.put(a.result(), resultVal);
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
            } else if ("_assert_fail".equals(c.funcName())) {
                throw new RuntimeException("Assertion failed");
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
        if (instr instanceof Instr.SpawnInstr s) {
            if (program != null) abmSpawn(s); else paramList.clear();
            return true;
        }
        if (instr instanceof Instr.MoveInstr m) {
            if (program != null) abmMove(m);
            return true;
        }
        if (instr instanceof Instr.StepInstr) {
            if (program != null) abmStep();
            return true;
        }
        if (instr instanceof Instr.DestroyInstr d) {
            if (program != null) abmDestroy(d);
            return true;
        }
        if (instr instanceof Instr.NeighborsInstr n) {
            if (program != null) abmNeighbors(n); else if (n.result() != null) store.put(n.result(), List.of());
            return true;
        }
        if (instr instanceof Instr.AbmCallInstr a) {
            if ("rand".equals(a.name())) {
                int lo, hi;
                if (a.args().size() == 0) {
                    lo = 0; hi = Integer.MAX_VALUE;
                } else if (a.args().size() == 1) {
                    lo = 0; hi = toInt(get(a.args().get(0)));
                } else {
                    lo = toInt(get(a.args().get(0)));
                    hi = toInt(get(a.args().get(1)));
                }
                int v = lo + (int) (Math.random() * ((long)(hi - lo) + 1));
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
            case ".": return left instanceof Map<?, ?> m && right != null ? m.get(right.toString()) : null;
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

    private static Object listIndex(Object listObj, Object indexObj) {
        if (!(listObj instanceof List<?> list)) return null;
        int i = toInt(indexObj);
        if (i < 0 || i >= list.size()) return null;
        return list.get(i);
    }

    private void abmStep() {
        if (worldName == null) return;
        FunctionIR pre = functions.get("world_" + worldName + "_pre");
        FunctionIR post = functions.get("world_" + worldName + "_post");
        if (pre != null) runFunction(pre, List.of(), worldStore);
        List<AgentHandle> toUpdate = new ArrayList<>(agents);
        for (AgentHandle agent : toUpdate) {
            if (!agents.contains(agent)) continue; // was destroyed
            FunctionIR update = functions.get("update_" + agent.typeName);
            if (update != null) {
                agent.store.put("self", agent);
                currentAgent = agent;
                runFunction(update, List.of(), agent.store);
            }
        }
        if (post != null) runFunction(post, List.of(), worldStore);
    }

    private void abmSpawn(Instr.SpawnInstr s) {
        String agentType = s.agentType();
        List<Object> args = new ArrayList<>();
        for (Operand o : s.args()) args.add(get(o));
        paramList.clear();
        Ast.AgentDeclNode decl = findAgentDecl(agentType);
        if (decl == null) return;
        Map<String, Object> agentStore = new HashMap<>();
        List<String> fieldNames = new ArrayList<>();
        for (Ast.VarDeclNode v : decl.fields()) {
            for (Ast.DeclaratorNode d : v.declarators()) {
                fieldNames.add(d.name());
                Object init = d.init() != null ? evalLiteralInit(d.init()) : defaultForType(v.dataType().baseTypeName());
                agentStore.put(d.name(), init);
            }
        }
        for (int i = 0; i < args.size() && i < fieldNames.size(); i++) {
            agentStore.put(fieldNames.get(i), args.get(i));
        }
        AgentHandle agent = new AgentHandle(nextAgentId++, agentType, agentStore);
        agents.add(agent);
    }

    private void abmMove(Instr.MoveInstr m) {
        if (currentAgent == null) return;
        Object x = get(m.x());
        Object y = get(m.y());
        Object z = m.z() != null ? get(m.z()) : null;
        currentAgent.store.put("x", x);
        currentAgent.store.put("y", y);
        if (z != null) currentAgent.store.put("z", z);
    }

    private void abmDestroy(Instr.DestroyInstr d) {
        Object target = get(d.target());
        if (target instanceof AgentHandle h) agents.remove(h);
    }

    private void abmNeighbors(Instr.NeighborsInstr n) {
        if (n.args().size() < 2 || currentAgent == null) {
            if (n.result() != null) store.put(n.result(), List.<AgentHandle>of());
            return;
        }
        Object selfObj = get(n.args().get(0));
        Object radiusObj = get(n.args().get(1));
        if (!(selfObj instanceof AgentHandle self)) {
            if (n.result() != null) store.put(n.result(), List.<AgentHandle>of());
            return;
        }
        int radius = toInt(radiusObj);
        int sx = toInt(self.store.get("x"));
        int sy = toInt(self.store.get("y"));
        List<AgentHandle> near = new ArrayList<>();
        for (AgentHandle a : agents) {
            if (a == self) continue;
            int ax = toInt(a.store.get("x"));
            int ay = toInt(a.store.get("y"));
            int dx = Math.abs(ax - sx);
            int dy = Math.abs(ay - sy);
            if (dx <= radius && dy <= radius) near.add(a);
        }
        if (n.result() != null) store.put(n.result(), near);
    }

    private Ast.AgentDeclNode findAgentDecl(String name) {
        if (program == null) return null;
        for (Ast.TypeDeclNode td : program.typeDecls()) {
            if (td instanceof Ast.AgentDeclNode a && a.name().equals(name)) return a;
        }
        return null;
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
