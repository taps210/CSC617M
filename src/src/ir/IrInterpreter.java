package src.ir;

import src.Ast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Interprets three-address IR: executes instructions one by one using a store and program counter.
 * Runs main(); function calls push a frame and run callee IR.
 * When ProgramNode is provided, ABM instructions (spawn, move, step, destroy, neighbors) use a minimal runtime.
 */
public final class IrInterpreter {
    // IR function-name conventions — must match IrBuilder's generated names
    private static final String FN_ASSERT_FAIL   = "_assert_fail";
    private static final String FN_RAND          = "rand";
    private static final String OP_ARRAY_ACCESS  = "[]";
    private static final String OP_MEMBER_ACCESS = ".";
    private static final String KEY_SELF         = "self";
    private static final String FN_PREFIX_UPDATE = "update_";
    private static final String FN_PREFIX_WORLD  = "world_";
    private static final String FN_SUFFIX_PRE    = "_pre";
    private static final String FN_SUFFIX_POST   = "_post";
    private static final String FN_PREFIX_ZONE         = "zone_";
    private static final String FN_PREFIX_AGENTMETHOD  = "agentmethod_";

    private final Map<String, FunctionIR> functions = new HashMap<>();
    private final Ast.ProgramNode program;
    private final Map<String, Object> constStore = new HashMap<>();
    private Map<String, Object> store;
    private FunctionIR currentFunc;
    private int pc;
    private Map<String, Integer> labelMap;
    private final List<Object> paramList = new ArrayList<>();
    private Object returnValue;
    private final InputStream in;
    private final BufferedReader inputReader;
    private final PrintStream out;
    private final DebugHook debugHook;
    private final Deque<DebugFrame> debugStack = new ArrayDeque<>();

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
        this(program, null, in, out, null);
    }

    public IrInterpreter(List<FunctionIR> funcs, Ast.ProgramNode program, InputStream in, PrintStream out) {
        this(funcs, program, in, out, null);
    }

    public IrInterpreter(List<FunctionIR> funcs, Ast.ProgramNode program, InputStream in, PrintStream out, DebugHook debugHook) {
        this.program = program;
        this.in = in != null ? in : System.in;
        this.inputReader = new BufferedReader(new InputStreamReader(this.in));
        this.out = out != null ? out : System.out;
        this.debugHook = debugHook;
        for (FunctionIR f : funcs) {
            functions.put(f.name(), f);
        }
        if (program != null) {
            for (Ast.ConstDeclNode c : program.constDecls()) {
                Object val = c.value() instanceof Ast.LiteralExprNode l ? l.value() : 0;
                constStore.put(c.name(), val);
            }
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
        List<Integer> lineTable = func.lineTable();
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

        // Push debug frame if debugging
        if (debugHook != null) {
            debugStack.push(new DebugFrame(func.name(), pc, -1, new HashMap<>(store)));
        }

        try {
            while (pc >= 0 && pc < instructions.size()) {
                Instr instr = instructions.get(pc);

                // Call debug hook before instruction execution
                if (debugHook != null) {
                    int line = pc < lineTable.size() ? lineTable.get(pc) : -1;
                    try {
                        debugHook.beforeInstruction(func, pc, line, Collections.unmodifiableMap(store), Collections.unmodifiableList(new ArrayList<>(debugStack)));
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Debugger interrupted execution", e);
                    }
                }

                boolean advance = execute(instr);
                if (returnValue != null && (instr instanceof Instr.ReturnInstr)) {
                    break;
                }
                if (advance) pc++;
            }
        } finally {
            // Pop debug frame if debugging
            if (debugHook != null && !debugStack.isEmpty()) {
                debugStack.pop();
            }

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

    /** Dispatches to the appropriate execute* handler. Returns true if PC should advance by 1. */
    private boolean execute(Instr instr) {
        if (instr instanceof Instr.LabelInstr)          return true;
        if (instr instanceof Instr.AssignConst a)       return executeAssignConst(a);
        if (instr instanceof Instr.AssignCopy a)        return executeAssignCopy(a);
        if (instr instanceof Instr.AssignBinary a)      return executeAssignBinary(a);
        if (instr instanceof Instr.AssignUnary a)       return executeAssignUnary(a);
        if (instr instanceof Instr.GotoInstr g)         return executeGoto(g);
        if (instr instanceof Instr.IfGotoInstr ig)      return executeIfGoto(ig);
        if (instr instanceof Instr.IfZeroGotoInstr iz)  return executeIfZeroGoto(iz);
        if (instr instanceof Instr.ParamInstr p)        return executeParam(p);
        if (instr instanceof Instr.CallInstr c)         return executeCall(c);
        if (instr instanceof Instr.ReturnInstr r)       return executeReturn(r);
        if (instr instanceof Instr.ReadInstr r)         return executeRead(r);
        if (instr instanceof Instr.PrintInstr p)        return executePrint(p);
        if (instr instanceof Instr.AllocArrayInstr a)   return executeAllocArray(a);
        if (instr instanceof Instr.ArrayStoreInstr a)   return executeArrayStore(a);
        if (instr instanceof Instr.SpawnInstr s)        return executeSpawn(s);
        if (instr instanceof Instr.MoveInstr m)         return executeMove(m);
        if (instr instanceof Instr.StepInstr)           return executeStep();
        if (instr instanceof Instr.DestroyInstr d)      return executeDestroy(d);
        if (instr instanceof Instr.NeighborsInstr n)    return executeNeighbors(n);
        if (instr instanceof Instr.AbmCallInstr a)      return executeAbmCall(a);
        if (instr instanceof Instr.ZoneEnterInstr z)       return executeZoneEnter(z);
        if (instr instanceof Instr.AgentMethodCallInstr a)  return executeAgentMethodCall(a);
        return true;
    }

    private boolean executeAssignConst(Instr.AssignConst a) {
        store.put(a.result(), a.value());
        return true;
    }

    private boolean executeAssignCopy(Instr.AssignCopy a) {
        store.put(a.result(), get(a.source()));
        return true;
    }

    private boolean executeAssignBinary(Instr.AssignBinary a) {
        Object left = get(a.left());
        Object right = get(a.right());
        Object resultVal;
        if (OP_ARRAY_ACCESS.equals(a.op())) {
            resultVal = listIndex(left, right);
        } else if (OP_MEMBER_ACCESS.equals(a.op())
                && a.right() instanceof Operand.VarOperand vr
                && "length".equals(vr.name())
                && left instanceof List<?> list) {
            resultVal = list.size();
        } else if (OP_MEMBER_ACCESS.equals(a.op())
                && a.right() instanceof Operand.VarOperand vr
                && left instanceof AgentHandle h) {
            resultVal = h.store.get(vr.name());
        } else {
            resultVal = evalBinary(left, a.op(), right);
        }
        store.put(a.result(), resultVal);
        return true;
    }

    private boolean executeAssignUnary(Instr.AssignUnary a) {
        store.put(a.result(), evalUnary(a.op(), get(a.operand())));
        return true;
    }

    private boolean executeGoto(Instr.GotoInstr g) {
        Integer target = labelMap.get(g.label());
        if (target != null) pc = target;
        return false;
    }

    private boolean executeIfGoto(Instr.IfGotoInstr ig) {
        if (truthy(get(ig.cond()))) {
            Integer target = labelMap.get(ig.label());
            if (target != null) pc = target;
            return false;
        }
        return true;
    }

    private boolean executeIfZeroGoto(Instr.IfZeroGotoInstr iz) {
        if (!truthy(get(iz.cond()))) {
            Integer target = labelMap.get(iz.label());
            if (target != null) pc = target;
            return false;
        }
        return true;
    }

    private boolean executeParam(Instr.ParamInstr p) {
        paramList.add(get(p.arg()));
        return true;
    }

    private boolean executeCall(Instr.CallInstr c) {
        FunctionIR callee = functions.get(c.funcName());
        List<Object> args = new ArrayList<>(paramList);
        paramList.clear();
        if (callee != null) {
            // Agent methods called from within the same agent's update/method share the agent store
            if (c.funcName().startsWith(FN_PREFIX_AGENTMETHOD) && currentAgent != null) {
                // Bind params into agent store before running, restore after
                Map<String, Object> agentStore = currentAgent.store;
                for (int i = 0; i < callee.paramNames().size() && i < args.size(); i++) {
                    agentStore.put(callee.paramNames().get(i), args.get(i));
                }
                runFunction(callee, args, agentStore);
            } else {
                runFunction(callee, args);
            }
            Object ret = returnValue;
            returnValue = null;
            if (c.result() != null && ret != null) store.put(c.result(), ret);
        } else if (FN_ASSERT_FAIL.equals(c.funcName())) {
            throw new RuntimeException("Assertion failed");
        }
        return true;
    }

    private boolean executeAgentMethodCall(Instr.AgentMethodCallInstr a) {
        Object handleObj = store.get(a.handle());
        List<Object> args = new ArrayList<>(paramList);
        paramList.clear();
        if (!(handleObj instanceof AgentHandle handle)) {
            if (a.result() != null) store.put(a.result(), 0);
            return true;
        }
        String irName = FN_PREFIX_AGENTMETHOD + handle.typeName + "_" + a.methodName();
        FunctionIR callee = functions.get(irName);
        if (callee == null) {
            if (a.result() != null) store.put(a.result(), 0);
            return true;
        }
        // Bind params into the neighbor's store, then run with that store
        for (int i = 0; i < callee.paramNames().size() && i < args.size(); i++) {
            handle.store.put(callee.paramNames().get(i), args.get(i));
        }
        AgentHandle prevAgent = currentAgent;
        currentAgent = handle;
        runFunction(callee, args, handle.store);
        currentAgent = prevAgent;
        Object ret = returnValue;
        returnValue = null;
        if (a.result() != null && ret != null) store.put(a.result(), ret);
        return true;
    }

    private boolean executeReturn(Instr.ReturnInstr r) {
        returnValue = r.value() != null ? get(r.value()) : null;
        return true;
    }

    private boolean executeRead(Instr.ReadInstr r) {
        String name = lvalueName(r.lvalue());
        try {
            String line = inputReader.readLine();
            if (line != null) store.put(name, parseInput(line));
        } catch (IOException e) {
            throw new RuntimeException("Read failed", e);
        }
        return true;
    }

    private boolean executePrint(Instr.PrintInstr p) {
        List<Object> vals = new ArrayList<>();
        for (Operand a : p.args()) vals.add(get(a));
        out.println(vals.stream().map(Objects::toString).reduce((a, b) -> a + " " + b).orElse(""));
        return true;
    }

    private boolean executeAllocArray(Instr.AllocArrayInstr a) {
        store.put(a.result(), new ArrayList<>(Collections.nCopies(a.size(), a.defaultVal())));
        return true;
    }

    private boolean executeArrayStore(Instr.ArrayStoreInstr a) {
        Object arrObj = store.get(a.arrayName());
        if (arrObj instanceof List<?>) {
            @SuppressWarnings("unchecked") List<Object> arr = (List<Object>) arrObj;
            int idx = toInt(get(a.index()));
            if (idx >= 0 && idx < arr.size()) arr.set(idx, get(a.value()));
        }
        return true;
    }

    private boolean executeSpawn(Instr.SpawnInstr s) {
        if (program != null) abmSpawn(s); else paramList.clear();
        return true;
    }

    private boolean executeMove(Instr.MoveInstr m) {
        if (program != null) abmMove(m);
        return true;
    }

    private boolean executeStep() {
        if (program != null) abmStep();
        return true;
    }

    private boolean executeDestroy(Instr.DestroyInstr d) {
        if (program != null) abmDestroy(d);
        return true;
    }

    private boolean executeNeighbors(Instr.NeighborsInstr n) {
        if (program != null) abmNeighbors(n);
        else if (n.result() != null) store.put(n.result(), List.of());
        return true;
    }

    private boolean executeAbmCall(Instr.AbmCallInstr a) {
        if (FN_RAND.equals(a.name())) {
            int lo, hi;
            if (a.args().isEmpty()) {
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

    private Object get(Operand o) {
        if (o instanceof Operand.ConstOperand c) return c.value();
        if (o instanceof Operand.VarOperand v) {
            Object val = store != null ? store.get(v.name()) : null;
            return val != null ? val : constStore.get(v.name());
        }
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
        return switch (op) {
            case "+"  -> add(left, right);
            case "-"  -> sub(left, right);
            case "*"  -> mul(left, right);
            case "/"  -> div(left, right);
            case "%"  -> mod(left, right);
            case "==" -> eq(left, right);
            case "!=" -> !eq(left, right);
            case "<"  -> toInt(left) < toInt(right);
            case "<=" -> toInt(left) <= toInt(right);
            case ">"  -> toInt(left) > toInt(right);
            case ">=" -> toInt(left) >= toInt(right);
            case "&&" -> truthy(left) && truthy(right);
            case "||" -> truthy(left) || truthy(right);
            case "."  -> left instanceof AgentHandle h && right != null ? h.store.get(right.toString())
                       : left instanceof Map<?, ?> m  && right != null ? m.get(right.toString()) : null;
            default   -> 0;
        };
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

    /** Copy world fields into the agent store so agents can read/write shared world state. */
    private void injectWorldFields(AgentHandle agent) {
        for (Map.Entry<String, Object> e : worldStore.entrySet()) {
            agent.store.put(e.getKey(), e.getValue());
        }
    }

    /** Write back any world-field keys that the agent may have modified. */
    private void flushWorldFields(AgentHandle agent) {
        for (String key : worldStore.keySet()) {
            if (agent.store.containsKey(key)) {
                worldStore.put(key, agent.store.get(key));
            }
        }
    }

    private void abmStep() {
        if (worldName == null) return;
        FunctionIR pre  = functions.get(FN_PREFIX_WORLD + worldName + FN_SUFFIX_PRE);
        FunctionIR post = functions.get(FN_PREFIX_WORLD + worldName + FN_SUFFIX_POST);
        if (pre != null) runFunction(pre, List.of(), worldStore);
        List<AgentHandle> toUpdate = new ArrayList<>(agents);
        for (AgentHandle agent : toUpdate) {
            if (!agents.contains(agent)) continue; // was destroyed
            FunctionIR update = functions.get(FN_PREFIX_UPDATE + agent.typeName);
            if (update != null) {
                agent.store.put(KEY_SELF, agent);
                injectWorldFields(agent);
                currentAgent = agent;
                runFunction(update, List.of(), agent.store);
                flushWorldFields(agent);
            }
            if (!agents.contains(agent)) continue; // destroyed during update
            Ast.AgentDeclNode decl = findAgentDecl(agent.typeName);
            if (decl != null) {
                for (Ast.ZoneDeclNode z : decl.zones()) {
                    FunctionIR zoneFunc = functions.get(FN_PREFIX_ZONE + agent.typeName + "_" + z.name());
                    if (zoneFunc != null) {
                        agent.store.put(KEY_SELF, agent);
                        injectWorldFields(agent);
                        currentAgent = agent;
                        runFunction(zoneFunc, List.of(), agent.store);
                        flushWorldFields(agent);
                    }
                    if (!agents.contains(agent)) break; // destroyed during a zone
                }
            }
        }
        if (post != null) runFunction(post, List.of(), worldStore);
    }

    private void abmSpawn(Instr.SpawnInstr s) {
        List<Object> args = new ArrayList<>();
        for (Operand o : s.args()) args.add(get(o));
        paramList.clear();
        Ast.AgentDeclNode decl = findAgentDecl(s.agentType());
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
        agentStore.putIfAbsent("x", 0);
        agentStore.putIfAbsent("y", 0);
        agentStore.putIfAbsent("z", 0);
        agents.add(new AgentHandle(nextAgentId++, s.agentType(), agentStore));
    }

    private void abmMove(Instr.MoveInstr m) {
        if (currentAgent == null) return;
        currentAgent.store.put("x", get(m.x()));
        currentAgent.store.put("y", get(m.y()));
        Object z = m.z() != null ? get(m.z()) : null;
        if (z != null) currentAgent.store.put("z", z);
    }

    private void abmDestroy(Instr.DestroyInstr d) {
        Object target = get(d.target());
        if (target instanceof AgentHandle h) agents.remove(h);
    }

    private static final List<String> DEFAULT_POSITION_FIELDS = List.of("x", "y", "z");

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
        List<String> fields = DEFAULT_POSITION_FIELDS;
        if (n.args().size() >= 3) {
            Object fieldsObj = get(n.args().get(2));
            if (fieldsObj instanceof List<?> fl && !fl.isEmpty()) {
                fields = fl.stream().map(Object::toString).toList();
            }
        }
        List<AgentHandle> near = new ArrayList<>();
        for (AgentHandle a : agents) {
            if (a == self) continue;
            boolean within = true;
            for (String field : fields) {
                if (Math.abs(toInt(a.store.get(field)) - toInt(self.store.get(field))) > radius) {
                    within = false;
                    break;
                }
            }
            if (within) near.add(a);
        }
        if (n.result() != null) store.put(n.result(), near);
    }

    private boolean executeZoneEnter(Instr.ZoneEnterInstr z) {
        if (currentAgent == null) {
            Integer target = labelMap.get(z.skipLabel());
            if (target != null) pc = target;
            return false;
        }
        int radius = toInt(get(z.radius()));
        AgentHandle self = currentAgent;
        for (AgentHandle a : agents) {
            if (a == self) continue;
            if (!z.targetType().equals(a.typeName)) continue;
            boolean within = true;
            for (String field : DEFAULT_POSITION_FIELDS) {
                if (Math.abs(toInt(a.store.get(field)) - toInt(self.store.get(field))) > radius) {
                    within = false;
                    break;
                }
            }
            if (within) return true; // found one — enter the zone block
        }
        // no matching agent within range — skip
        Integer target = labelMap.get(z.skipLabel());
        if (target != null) pc = target;
        return false;
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
