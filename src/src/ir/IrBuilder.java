package src.ir;

import static src.Ast.*;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Builds three-address IR from the AST. One function (or main) at a time.
 */
public final class IrBuilder {
    // IR function-name conventions — must match IrInterpreter's constants
    private static final String FN_ASSERT_FAIL   = "_assert_fail";
    private static final String FN_NEIGHBORS     = "neighbors";
    private static final String OP_ARRAY_ACCESS  = "[]";
    private static final String KEY_SELF         = "self";
    private static final String FN_PREFIX_UPDATE = "update_";
    private static final String FN_PREFIX_WORLD  = "world_";
    private static final String FN_SUFFIX_PRE    = "_pre";
    private static final String FN_SUFFIX_POST   = "_post";
    public  static final String FN_PREFIX_ZONE         = "zone_";
    public  static final String FN_PREFIX_AGENTMETHOD  = "agentmethod_";

    private final List<Instr> instructions = new ArrayList<>();
    private int tempCounter = 0;
    private int labelCounter = 0;
    /** (breakLabel, continueLabel) for current loop. */
    private final Deque<String[]> loopLabels = new LinkedList<>();
    /** Const name → literal value, for inline ConstOperand injection. */
    private final Map<String, Object> constValues;
    /** Name of the agent currently being compiled (null outside agent context). */
    private String currentAgentName = null;
    /** Simple method names of the current agent — used to qualify calls from update/method bodies. */
    private java.util.Set<String> currentAgentMethodNames = java.util.Set.of();

    private IrBuilder(Map<String, Object> constValues) {
        this.constValues = constValues;
    }

    public static List<FunctionIR> buildProgram(ProgramNode program) {
        Map<String, Object> constValues = new HashMap<>();
        for (ConstDeclNode c : program.constDecls()) {
            if (c.value() instanceof LiteralExprNode l) constValues.put(c.name(), l.value());
        }
        List<FunctionIR> out = new ArrayList<>();
        for (FuncDeclNode f : program.funcDecls()) {
            IrBuilder b = new IrBuilder(constValues);
            b.buildFunctionBody(f.name(), f.body());
            List<String> paramNames = f.params().stream().map(p -> p.name()).toList();
            out.add(new FunctionIR(f.name(), paramNames, new ArrayList<>(b.instructions)));
        }
        if (program.main() != null) {
            IrBuilder b = new IrBuilder(constValues);
            b.buildFunctionBody("main", program.main().body());
            out.add(new FunctionIR("main", List.of(), new ArrayList<>(b.instructions)));
        }
        for (var td : program.typeDecls()) {
            if (td instanceof AgentDeclNode a) {
                java.util.Set<String> methodNames = a.methods().stream()
                        .map(FuncDeclNode::name)
                        .collect(java.util.stream.Collectors.toSet());
                // Compile update block with agent context so method calls get qualified
                if (a.updateBlock() != null) {
                    String updateName = FN_PREFIX_UPDATE + a.name();
                    IrBuilder b = new IrBuilder(constValues);
                    b.currentAgentName = a.name();
                    b.currentAgentMethodNames = methodNames;
                    b.buildFunctionBody(updateName, a.updateBlock());
                    out.add(new FunctionIR(updateName, List.of(), new ArrayList<>(b.instructions)));
                }
                // Compile agent methods
                for (FuncDeclNode m : a.methods()) {
                    String methodIrName = FN_PREFIX_AGENTMETHOD + a.name() + "_" + m.name();
                    List<String> paramNames = m.params().stream().map(ParamNode::name).toList();
                    IrBuilder b = new IrBuilder(constValues);
                    b.currentAgentName = a.name();
                    b.currentAgentMethodNames = methodNames;
                    b.buildFunctionBody(methodIrName, m.body());
                    out.add(new FunctionIR(methodIrName, paramNames, new ArrayList<>(b.instructions)));
                }
            }
            if (td instanceof AgentDeclNode a) {
                for (ZoneDeclNode z : a.zones()) {
                    String zoneName = FN_PREFIX_ZONE + a.name() + "_" + z.name();
                    IrBuilder b = new IrBuilder(constValues);
                    b.buildZoneBody(z.condition(), z.targetIdent(), z.block());
                    out.add(new FunctionIR(zoneName, List.of(), new ArrayList<>(b.instructions)));
                }
            }
            if (td instanceof WorldDeclNode w) {
                if (w.preBlock() != null) {
                    String preName = FN_PREFIX_WORLD + w.name() + FN_SUFFIX_PRE;
                    IrBuilder b = new IrBuilder(constValues);
                    b.buildFunctionBody(preName, w.preBlock());
                    out.add(new FunctionIR(preName, List.of(), new ArrayList<>(b.instructions)));
                }
                if (w.postBlock() != null) {
                    String postName = FN_PREFIX_WORLD + w.name() + FN_SUFFIX_POST;
                    IrBuilder b = new IrBuilder(constValues);
                    b.buildFunctionBody(postName, w.postBlock());
                    out.add(new FunctionIR(postName, List.of(), new ArrayList<>(b.instructions)));
                }
            }
        }
        return out;
    }

    private String nextTemp() {
        return "t" + (++tempCounter);
    }

    private String nextLabel() {
        return "L" + (++labelCounter);
    }

    private void emit(Instr i) {
        instructions.add(i);
    }

    private void buildZoneBody(ExprNode radiusExpr, String targetType, BlockNode body) {
        Operand radius = genExpr(radiusExpr);
        String LEnd = nextLabel();
        emit(new Instr.ZoneEnterInstr(radius, targetType, LEnd));
        genBlock(body);
        emit(new Instr.LabelInstr(LEnd));
    }

    private void buildFunctionBody(String funcName, BlockNode body) {
        if (body == null) return;
        for (VarDeclNode v : body.varDecls()) {
            for (DeclaratorNode d : v.declarators()) {
                if (!d.arrayDims().isEmpty()) {
                    int size = d.arrayDims().get(0);
                    emit(new Instr.AllocArrayInstr(d.name(), size, 0));
                } else if (d.init() != null) {
                    Operand val = genExpr(d.init());
                    emit(new Instr.AssignCopy(d.name(), val));
                }
            }
        }
        for (StatementNode s : body.statements()) {
            genStmt(s);
        }
    }

    private void genStmt(StatementNode s) {
        if (s instanceof AssignStmtNode n) {
            ExprNode lv = n.lvalue();
            if (lv instanceof BinaryExprNode b && "[]".equals(b.op())) {
                String arrayName = lvalueName(b.left());
                Operand index = genExpr(b.right());
                Operand val = genExpr(n.value());
                emit(new Instr.ArrayStoreInstr(arrayName, index, val));
            } else {
                Operand val = genExpr(n.value());
                String target = lvalueName(lv);
                emit(new Instr.AssignCopy(target, val));
            }
            return;
        }
        if (s instanceof CallStmtNode n) {
            String funcName = qualifyIfAgentMethod(n.name());
            for (ExprNode a : n.args()) emit(new Instr.ParamInstr(genExpr(a)));
            emit(new Instr.CallInstr(funcName, null));
            return;
        }
        if (s instanceof IfStmtNode n) {
            Operand cond = genExpr(n.condition());
            String LElse = nextLabel();
            String LEnd = nextLabel();
            emit(new Instr.IfZeroGotoInstr(cond, LElse));
            genStmt(n.thenBranch());
            emit(new Instr.GotoInstr(LEnd));
            emit(new Instr.LabelInstr(LElse));
            if (n.elseBranch() != null) genStmt(n.elseBranch());
            emit(new Instr.LabelInstr(LEnd));
            return;
        }
        if (s instanceof WhileStmtNode n) {
            String LStart = nextLabel();
            String LEnd = nextLabel();
            loopLabels.push(new String[] { LEnd, LStart });
            emit(new Instr.LabelInstr(LStart));
            Operand cond = genExpr(n.condition());
            emit(new Instr.IfZeroGotoInstr(cond, LEnd)); // if cond is false, exit loop
            genStmt(n.body());
            emit(new Instr.GotoInstr(LStart));
            emit(new Instr.LabelInstr(LEnd));
            loopLabels.pop();
            return;
        }
        if (s instanceof ForStmtNode n) {
            genForInit(n.init());
            String LStart    = nextLabel();
            String LContinue = nextLabel(); // continue jumps here, before the update
            String LEnd      = nextLabel();
            loopLabels.push(new String[] { LEnd, LContinue });
            emit(new Instr.LabelInstr(LStart));
            if (n.condition() != null) {
                Operand cond = genExpr(n.condition());
                emit(new Instr.IfZeroGotoInstr(cond, LEnd));
            }
            genStmt(n.body());
            emit(new Instr.LabelInstr(LContinue));
            for (AssignStmtNode a : n.update()) genStmt(a);
            emit(new Instr.GotoInstr(LStart));
            emit(new Instr.LabelInstr(LEnd));
            loopLabels.pop();
            return;
        }
        if (s instanceof RepeatUntilStmtNode n) {
            String LStart = nextLabel();
            String LEnd = nextLabel();
            loopLabels.push(new String[] { LEnd, LStart });
            emit(new Instr.LabelInstr(LStart));
            genBlock(n.block());
            Operand cond = genExpr(n.condition());
            emit(new Instr.IfZeroGotoInstr(cond, LStart)); // if condition is false, repeat
            emit(new Instr.LabelInstr(LEnd));
            loopLabels.pop();
            return;
        }
        if (s instanceof ReturnStmtNode n) {
            Operand val = n.value() != null ? genExpr(n.value()) : null;
            emit(new Instr.ReturnInstr(val));
            return;
        }
        if (s instanceof BreakStmtNode) {
            if (!loopLabels.isEmpty()) emit(new Instr.GotoInstr(loopLabels.peek()[0]));
            return;
        }
        if (s instanceof ContinueStmtNode) {
            if (!loopLabels.isEmpty()) emit(new Instr.GotoInstr(loopLabels.peek()[1]));
            return;
        }
        if (s instanceof AssertStmtNode n) {
            Operand cond = genExpr(n.condition());
            String L = nextLabel();
            emit(new Instr.IfGotoInstr(cond, L));
            emit(new Instr.CallInstr(FN_ASSERT_FAIL, null));
            emit(new Instr.LabelInstr(L));
            return;
        }
        if (s instanceof ReadStmtNode n) {
            String target = lvalueName(n.lvalue());
            emit(new Instr.ReadInstr(Operand.var(target)));
            return;
        }
        if (s instanceof PrintStmtNode n) {
            List<Operand> args = new ArrayList<>();
            for (ExprNode a : n.args()) args.add(genExpr(a));
            emit(new Instr.PrintInstr(args));
            return;
        }
        if (s instanceof BlockStmtNode n) {
            genBlock(n.block());
            return;
        }
        if (s instanceof SpawnStmtNode sn) {
            List<Operand> args = new ArrayList<>();
            for (ExprNode a : sn.args()) args.add(genExpr(a));
            emit(new Instr.SpawnInstr(sn.agentType(), args));
            return;
        }
        if (s instanceof MoveStmtNode mn) {
            Operand z = mn.z() != null ? genExpr(mn.z()) : Operand.constant(0);
            emit(new Instr.MoveInstr(genExpr(mn.x()), genExpr(mn.y()), z));
            return;
        }
        if (s instanceof StepStmtNode sn) {
            Operand arg = sn.arg() != null ? genExpr(sn.arg()) : null;
            emit(new Instr.StepInstr(arg));
            return;
        }
        if (s instanceof DestroyStmtNode dn) {
            emit(new Instr.DestroyInstr(genExpr(dn.target())));
            return;
        }
        if (s instanceof AbmCallStmtNode an) {
            List<Operand> args = new ArrayList<>();
            for (ExprNode a : an.args()) args.add(genExpr(a));
            if (FN_NEIGHBORS.equals(an.name())) {
                emit(new Instr.NeighborsInstr(args, null));
            } else {
                emit(new Instr.AbmCallInstr(an.name(), args, null));
            }
            return;
        }
    }

    private void genBlock(BlockNode block) {
        if (block == null) return;
        for (VarDeclNode v : block.varDecls()) {
            for (DeclaratorNode d : v.declarators()) {
                if (!d.arrayDims().isEmpty()) {
                    int size = d.arrayDims().get(0);
                    emit(new Instr.AllocArrayInstr(d.name(), size, 0));
                } else if (d.init() != null) {
                    emit(new Instr.AssignCopy(d.name(), genExpr(d.init())));
                }
            }
        }
        for (StatementNode st : block.statements()) genStmt(st);
    }

    private void genForInit(ForInitNode init) {
        if (init instanceof ForInitVarDecl v) {
            for (DeclaratorNode d : v.declarators()) {
                if (d.init() != null) {
                    emit(new Instr.AssignCopy(d.name(), genExpr(d.init())));
                }
            }
            return;
        }
        if (init instanceof ForInitAssignList a) {
            for (AssignStmtNode as : a.assignStmts()) genStmt(as);
        }
    }

    private String lvalueName(ExprNode lvalue) {
        if (lvalue instanceof IdentExprNode n) return n.name();
        if (lvalue instanceof LvalueExprNode n) return n.baseName();
        if (lvalue instanceof SelfFieldExprNode n) return n.fieldName();
        return "?";
    }

    private Operand genExpr(ExprNode e) {
        if (e instanceof LiteralExprNode n) {
            String t = nextTemp();
            emit(new Instr.AssignConst(t, n.value()));
            return Operand.temp(t);
        }
        if (e instanceof IdentExprNode n) {
            if (constValues.containsKey(n.name())) return Operand.constant(constValues.get(n.name()));
            return Operand.var(n.name());
        }
        if (e instanceof SelfExprNode) {
            return Operand.var(KEY_SELF);
        }
        if (e instanceof SelfFieldExprNode n) {
            return Operand.var(n.fieldName());
        }
        if (e instanceof NullExprNode) {
            String t = nextTemp();
            emit(new Instr.AssignConst(t, null));
            return Operand.temp(t);
        }
        if (e instanceof ParenExprNode n) {
            return genExpr(n.inner());
        }
        if (e instanceof BinaryExprNode n) {
            Operand left = genExpr(n.left());
            Operand right = genExpr(n.right());
            String result = nextTemp();
            emit(new Instr.AssignBinary(result, left, n.op(), right));
            return Operand.temp(result);
        }
        if (e instanceof UnaryExprNode n) {
            Operand op = genExpr(n.operand());
            String result = nextTemp();
            emit(new Instr.AssignUnary(result, n.op(), op));
            return Operand.temp(result);
        }
        if (e instanceof TernaryExprNode n) {
            Operand cond = genExpr(n.condition());
            String LElse = nextLabel();
            String LEnd = nextLabel();
            String result = nextTemp();
            emit(new Instr.IfZeroGotoInstr(cond, LElse));
            Operand thenVal = genExpr(n.thenExpr());
            emit(new Instr.AssignCopy(result, thenVal));
            emit(new Instr.GotoInstr(LEnd));
            emit(new Instr.LabelInstr(LElse));
            Operand elseVal = genExpr(n.elseExpr());
            emit(new Instr.AssignCopy(result, elseVal));
            emit(new Instr.LabelInstr(LEnd));
            return Operand.temp(result);
        }
        if (e instanceof CallExprNode n) {
            String funcName = qualifyIfAgentMethod(n.name());
            for (ExprNode a : n.args()) emit(new Instr.ParamInstr(genExpr(a)));
            String result = nextTemp();
            emit(new Instr.CallInstr(funcName, result));
            return Operand.temp(result);
        }
        if (e instanceof MethodCallExprNode n) {
            Operand targetOp = genExpr(n.target());
            String targetVar;
            if (targetOp instanceof Operand.VarOperand v) {
                targetVar = v.name();
            } else {
                targetVar = nextTemp();
                emit(new Instr.AssignCopy(targetVar, targetOp));
            }
            for (ExprNode a : n.args()) emit(new Instr.ParamInstr(genExpr(a)));
            String result = nextTemp();
            emit(new Instr.AgentMethodCallInstr(targetVar, n.methodName(), result));
            return Operand.temp(result);
        }
        if (e instanceof AbmCallExprNode n) {
            List<Operand> args = new ArrayList<>();
            for (ExprNode a : n.args()) args.add(genExpr(a));
            String result = nextTemp();
            if (FN_NEIGHBORS.equals(n.name())) {
                emit(new Instr.NeighborsInstr(args, result));
            } else {
                emit(new Instr.AbmCallInstr(n.name(), args, result));
            }
            return Operand.temp(result);
        }
        if (e instanceof LvalueExprNode n) {
            return Operand.var(n.baseName());
        }
        if (e instanceof PlaceholderExprNode) {
            String t = nextTemp();
            emit(new Instr.AssignConst(t, 0));
            return Operand.temp(t);
        }
        String t = nextTemp();
        emit(new Instr.AssignConst(t, 0));
        return Operand.temp(t);
    }

    private String qualifyIfAgentMethod(String name) {
        return (currentAgentName != null && currentAgentMethodNames.contains(name))
                ? FN_PREFIX_AGENTMETHOD + currentAgentName + "_" + name
                : name;
    }
}
