package src.gui.analysis;

import static src.Ast.*;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

/**
 * Builds a Swing DefaultTreeModel from a ProgramNode for parse tree visualization.
 * Single-file adapter: each AST node becomes a tree node with a short label and children.
 */
public final class AstTreeModel {

    public static DefaultTreeModel from(ProgramNode root) {
        return new DefaultTreeModel(fromProgram(root));
    }

    private static String L(SourceSpan s) { return s == null ? "" : " (L" + s.line() + ")"; }
    private static DefaultMutableTreeNode n(String label) { return new DefaultMutableTreeNode(label); }
    private static DefaultMutableTreeNode n(String label, SourceSpan loc) { return n(label + L(loc)); }

    private static void addE(DefaultMutableTreeNode p, ExprNode e) { if (e != null) p.add(fromExpr(e)); }
    private static void addS(DefaultMutableTreeNode p, StatementNode s) { if (s != null) p.add(fromStatement(s)); }
    private static void addB(DefaultMutableTreeNode p, BlockNode b) { if (b != null) p.add(fromBlock(b)); }
    private static DefaultMutableTreeNode wrap(String label, DefaultMutableTreeNode child) {
        DefaultMutableTreeNode w = n(label); w.add(child); return w;
    }
    private static <T> DefaultMutableTreeNode list(String listLabel, List<T> list, java.util.function.Function<T, DefaultMutableTreeNode> mapper) {
        DefaultMutableTreeNode r = n(listLabel + " (" + list.size() + ")");
        for (T item : list) r.add(mapper.apply(item));
        return r;
    }


    private static DefaultMutableTreeNode fromProgram(ProgramNode x) {
        DefaultMutableTreeNode r = n("Program", x.location());
        r.add(list("uses", x.uses(), AstTreeModel::fromUse));
        r.add(list("typeDecls", x.typeDecls(), AstTreeModel::fromTypeDecl));
        r.add(list("constDecls", x.constDecls(), AstTreeModel::fromConstDecl));
        r.add(list("globalVarDecls", x.globalVarDecls(), AstTreeModel::fromVarDecl));
        r.add(list("funcDecls", x.funcDecls(), AstTreeModel::fromFuncDecl));
        r.add(fromMain(x.main()));
        return r;
    }

    private static DefaultMutableTreeNode fromUse(UseNode x) { return n("Use: " + String.join(".", x.filenameSegments()), x.location()); }

    private static DefaultMutableTreeNode fromTypeDecl(TypeDeclNode x) {
        if (x instanceof TypeAliasNode t) return fromTypeAlias(t);
        if (x instanceof AgentDeclNode a) return fromAgentDecl(a);
        if (x instanceof WorldDeclNode w) return fromWorldDecl(w);
        return n("TypeDecl?");
    }

    private static DefaultMutableTreeNode fromTypeAlias(TypeAliasNode x) {
        DefaultMutableTreeNode r = n("Type: " + x.typeName(), x.location()); r.add(fromRecordType(x.recordType())); return r;
    }
    private static DefaultMutableTreeNode fromRecordType(RecordTypeNode x) {
        DefaultMutableTreeNode r = n("Record", x.location());
        for (VarDeclNode v : x.fields()) r.add(fromVarDecl(v));
        return r;
    }

    private static DefaultMutableTreeNode fromAgentDecl(AgentDeclNode x) {
        DefaultMutableTreeNode r = n("Agent: " + x.name(), x.location());
        r.add(list("fields", x.fields(), AstTreeModel::fromVarDecl));
        r.add(list("zones", x.zones(), AstTreeModel::fromZoneDecl));
        r.add(wrap("update", fromBlock(x.updateBlock())));
        return r;
    }
    private static DefaultMutableTreeNode fromZoneDecl(ZoneDeclNode x) {
        DefaultMutableTreeNode r = n("Zone: " + x.name() + " -> " + x.targetIdent(), x.location());
        r.add(fromExpr(x.condition())); r.add(fromBlock(x.block()));
        return r;
    }
    private static DefaultMutableTreeNode fromWorldDecl(WorldDeclNode x) {
        DefaultMutableTreeNode r = n("World: " + x.name(), x.location());
        r.add(list("fields", x.fields(), AstTreeModel::fromVarDecl));
        if (x.preBlock() != null) r.add(wrap("pre", fromBlock(x.preBlock())));
        if (x.postBlock() != null) r.add(wrap("post", fromBlock(x.postBlock())));
        return r;
    }

    private static DefaultMutableTreeNode fromConstDecl(ConstDeclNode x) {
        DefaultMutableTreeNode r = n("Const: " + x.name(), x.location()); r.add(fromExpr(x.value())); return r;
    }
    private static DefaultMutableTreeNode fromVarDecl(VarDeclNode x) {
        DefaultMutableTreeNode r = n("VarDecl", x.location());
        r.add(n("type: " + x.dataType().baseTypeName() + "*".repeat(x.dataType().pointerLevel())));
        for (DeclaratorNode d : x.declarators()) r.add(fromDeclarator(d));
        return r;
    }
    private static DefaultMutableTreeNode fromDeclarator(DeclaratorNode x) {
        String lb = x.name();
        if (!x.arrayDims().isEmpty()) lb += "[" + String.join("][", x.arrayDims().stream().map(String::valueOf).toList()) + "]";
        if (x.init() != null) lb += " = ...";
        DefaultMutableTreeNode r = n(lb, x.location()); addE(r, x.init()); return r;
    }
    private static DefaultMutableTreeNode fromParam(ParamNode x) {
        DefaultMutableTreeNode r = n(x.name(), x.location()); r.add(n("type: " + x.dataType().baseTypeName())); return r;
    }
    private static DefaultMutableTreeNode fromFuncDecl(FuncDeclNode x) {
        String ret = x.returnType() == null ? "void" : x.returnType().baseTypeName();
        DefaultMutableTreeNode r = n("Func: " + x.name() + " -> " + ret, x.location());
        for (ParamNode p : x.params()) r.add(fromParam(p));
        r.add(fromBlock(x.body())); return r;
    }
    private static DefaultMutableTreeNode fromMain(MainFunctionNode x) {
        DefaultMutableTreeNode r = n("main", x.location()); r.add(fromBlock(x.body())); return r;
    }
    private static DefaultMutableTreeNode fromBlock(BlockNode x) {
        DefaultMutableTreeNode r = n("Block", x.location());
        if (!x.varDecls().isEmpty()) r.add(list("varDecls", x.varDecls(), AstTreeModel::fromVarDecl));
        for (StatementNode s : x.statements()) r.add(fromStatement(s));
        return r;
    }

    private static DefaultMutableTreeNode fromStatement(StatementNode s) {
        if (s instanceof AssignStmtNode x) { DefaultMutableTreeNode r = n("Assign", x.location()); addE(r, x.lvalue()); addE(r, x.value()); return r; }
        if (s instanceof CallStmtNode x) { DefaultMutableTreeNode r = n("Call: " + x.name(), x.location()); for (ExprNode e : x.args()) addE(r, e); return r; }
        if (s instanceof IfStmtNode x) {
            DefaultMutableTreeNode r = n("If", x.location()); r.add(fromExpr(x.condition()));
            r.add(wrap("then", fromStatement(x.thenBranch())));
            if (x.elseBranch() != null) r.add(wrap("else", fromStatement(x.elseBranch())));
            return r;
        }
        if (s instanceof WhileStmtNode x) { DefaultMutableTreeNode r = n("While", x.location()); addE(r, x.condition()); addS(r, x.body()); return r; }
        if (s instanceof ForStmtNode x) {
            DefaultMutableTreeNode r = n("For", x.location());
            if (x.init() != null) r.add(fromForInit(x.init()));
            addE(r, x.condition());
            for (AssignStmtNode a : x.update()) addS(r, a);
            addS(r, x.body()); return r;
        }
        if (s instanceof RepeatUntilStmtNode x) { DefaultMutableTreeNode r = n("RepeatUntil", x.location()); r.add(fromBlock(x.block())); addE(r, x.condition()); return r; }
        if (s instanceof ReturnStmtNode x) { DefaultMutableTreeNode r = n("Return", x.location()); addE(r, x.value()); return r; }
        if (s instanceof BreakStmtNode x) return n("Break", x.location());
        if (s instanceof ContinueStmtNode x) return n("Continue", x.location());
        if (s instanceof AssertStmtNode x) { DefaultMutableTreeNode r = n("Assert", x.location()); addE(r, x.condition()); return r; }
        if (s instanceof ReadStmtNode x) { DefaultMutableTreeNode r = n("Read", x.location()); addE(r, x.lvalue()); return r; }
        if (s instanceof PrintStmtNode x) { DefaultMutableTreeNode r = n("Print", x.location()); for (ExprNode e : x.args()) addE(r, e); return r; }
        if (s instanceof SpawnStmtNode x) { DefaultMutableTreeNode r = n("Spawn: " + x.agentType(), x.location()); for (ExprNode e : x.args()) addE(r, e); return r; }
        if (s instanceof MoveStmtNode x) { DefaultMutableTreeNode r = n("Move", x.location()); addE(r, x.x()); addE(r, x.y()); addE(r, x.z()); return r; }
        if (s instanceof StepStmtNode x) { DefaultMutableTreeNode r = n("Step", x.location()); addE(r, x.arg()); return r; }
        if (s instanceof DestroyStmtNode x) { DefaultMutableTreeNode r = n("Destroy", x.location()); addE(r, x.target()); return r; }
        if (s instanceof AbmCallStmtNode x) { DefaultMutableTreeNode r = n("AbmCall: " + x.name(), x.location()); for (ExprNode e : x.args()) addE(r, e); return r; }
        if (s instanceof BlockStmtNode x) { DefaultMutableTreeNode r = n("BlockStmt", x.location()); r.add(fromBlock(x.block())); return r; }
        if (s instanceof PlaceholderStmtNode x) return n("PlaceholderStmt", x.location());
        return n("Stmt?");
    }

    private static DefaultMutableTreeNode fromForInit(ForInitNode x) {
        if (x instanceof ForInitVarDecl v) {
            DefaultMutableTreeNode r = n("ForInit (var)"); r.add(n("type: " + v.dataType().baseTypeName()));
            for (DeclaratorNode d : v.declarators()) r.add(fromDeclarator(d)); return r;
        }
        if (x instanceof ForInitAssignList a) { DefaultMutableTreeNode r = n("ForInit (assign list)"); for (AssignStmtNode s : a.assignStmts()) addS(r, s); return r; }
        return n("ForInit?");
    }

    private static DefaultMutableTreeNode fromExpr(ExprNode e) {
        if (e instanceof BinaryExprNode x) { DefaultMutableTreeNode r = n("Binary: " + x.op(), x.location()); r.add(fromExpr(x.left())); r.add(fromExpr(x.right())); return r; }
        if (e instanceof UnaryExprNode x) { DefaultMutableTreeNode r = n("Unary: " + x.op(), x.location()); addE(r, x.operand()); return r; }
        if (e instanceof CallExprNode x) { DefaultMutableTreeNode r = n("Call: " + x.name(), x.location()); for (ExprNode a : x.args()) addE(r, a); return r; }
        if (e instanceof LiteralExprNode x) return n("Literal: " + x.value(), x.location());
        if (e instanceof IdentExprNode x) return n("Ident: " + x.name(), x.location());
        if (e instanceof SelfFieldExprNode x) return n("Self." + x.fieldName(), x.location());
        if (e instanceof SelfExprNode x) return n("self", x.location());
        if (e instanceof NullExprNode x) return n("null", x.location());
        if (e instanceof ParenExprNode x) { DefaultMutableTreeNode r = n("Paren", x.location()); addE(r, x.inner()); return r; }
        if (e instanceof TernaryExprNode x) { DefaultMutableTreeNode r = n("Ternary", x.location()); addE(r, x.condition()); addE(r, x.thenExpr()); addE(r, x.elseExpr()); return r; }
        if (e instanceof LvalueExprNode x) return n("Lvalue: " + x.baseName(), x.location());
        if (e instanceof AbmCallExprNode x) { DefaultMutableTreeNode r = n("AbmCall: " + x.name(), x.location()); for (ExprNode a : x.args()) addE(r, a); return r; }
        if (e instanceof PlaceholderExprNode x) return n("PlaceholderExpr", x.location());
        return n("Expr?");
    }
}
