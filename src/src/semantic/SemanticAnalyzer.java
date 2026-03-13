package src.semantic;

import static src.Ast.*;
import src.errors.SemanticError;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic analysis: symbol table construction, name resolution, type checking, language rules.
 * Collects errors and returns them (no throw on first error).
 */
public class SemanticAnalyzer {
    private final List<SemanticError> errors = new ArrayList<>();
    private final SymbolTable table = new SymbolTable();
    private int agentDepth = 0;

    public List<SemanticError> analyze(ProgramNode program) {
        errors.clear();
        table.pushScope();
        defineBuiltinTypes();
        for (TypeDeclNode td : program.typeDecls()) visitTypeDecl(td);
        for (ConstDeclNode c : program.constDecls()) visitConstDecl(c);
        for (VarDeclNode v : program.globalVarDecls()) visitVarDecl(v);
        for (FuncDeclNode f : program.funcDecls()) visitFuncDecl(f);
        checkMain(program.main());
        if (program.main() != null) visitMain(program.main());
        table.popScope();
        return List.copyOf(errors);
    }

    private void defineBuiltinTypes() {
        SourceSpan zero = SourceSpan.of(0, 0);
        table.define(SymbolTable.Symbol.type("int", new DataTypeNode(zero, "int", 0), zero));
        table.define(SymbolTable.Symbol.type("float", new DataTypeNode(zero, "float", 0), zero));
        table.define(SymbolTable.Symbol.type("char", new DataTypeNode(zero, "char", 0), zero));
        table.define(SymbolTable.Symbol.type("string", new DataTypeNode(zero, "string", 0), zero));
        table.define(SymbolTable.Symbol.type("bool", new DataTypeNode(zero, "bool", 0), zero));
        table.define(SymbolTable.Symbol.type("void", new DataTypeNode(zero, "void", 0), zero));
    }

    private void visitTypeDecl(TypeDeclNode n) {
        if (n instanceof TypeAliasNode a) {
            SymbolTable.Symbol sym = SymbolTable.Symbol.type(a.typeName(), null, a.location());
            if (!table.define(sym)) error(a.location(), "Duplicate type name: " + a.typeName());
            return;
        }
        if (n instanceof AgentDeclNode a) {
            if (!table.define(SymbolTable.Symbol.agent(a.name(), a.location()))) error(a.location(), "Duplicate agent name: " + a.name());
            table.pushScope();
            agentDepth++;
            for (VarDeclNode f : a.fields()) visitVarDecl(f);
            for (ZoneDeclNode z : a.zones()) visitZoneDecl(z);
            visitBlock(a.updateBlock());
            agentDepth--;
            table.popScope();
            return;
        }
        if (n instanceof WorldDeclNode w) {
            if (!table.define(SymbolTable.Symbol.world(w.name(), w.location()))) error(w.location(), "Duplicate world name: " + w.name());
            table.pushScope();
            for (VarDeclNode f : w.fields()) visitVarDecl(f);
            if (w.preBlock() != null) visitBlock(w.preBlock());
            if (w.postBlock() != null) visitBlock(w.postBlock());
            table.popScope();
        }
    }

    private void visitZoneDecl(ZoneDeclNode z) {
        visitExpr(z.condition());
        visitBlock(z.block());
    }

    private void visitConstDecl(ConstDeclNode n) {
        if (table.definedInCurrentScope(n.name())) {
            error(n.location(), "Duplicate constant: " + n.name());
            return;
        }
        DataTypeNode type = typeOfExpr(n.value());
        table.define(SymbolTable.Symbol.constant(n.name(), type, n.location()));
    }

    private void visitVarDecl(VarDeclNode n) {
        for (DeclaratorNode d : n.declarators()) {
            if (table.definedInCurrentScope(d.name())) error(d.location(), "Duplicate variable: " + d.name());
            else table.define(SymbolTable.Symbol.variable(d.name(), n.dataType(), d.location()));
            if (d.init() != null) visitExpr(d.init());
        }
    }

    private void visitFuncDecl(FuncDeclNode n) {
        if (table.definedInCurrentScope(n.name())) {
            error(n.location(), "Duplicate function: " + n.name());
            return;
        }
        List<DataTypeNode> paramTypes = n.params().stream().map(ParamNode::dataType).toList();
        table.define(SymbolTable.Symbol.function(n.name(), n.returnType(), paramTypes, n.location()));
        table.pushScope();
        for (ParamNode p : n.params()) {
            if (table.definedInCurrentScope(p.name())) error(p.location(), "Duplicate parameter: " + p.name());
            else table.define(SymbolTable.Symbol.variable(p.name(), p.dataType(), p.location()));
        }
        visitBlock(n.body());
        table.popScope();
    }

    private void checkMain(MainFunctionNode main) {
        if (main == null) {
            errors.add(new SemanticError(0, 0, "Program must have exactly one void main() with no parameters"));
            return;
        }
        // Signature is enforced by parser (void main()), no extra check needed unless we want to double-check
    }

    private void visitMain(MainFunctionNode main) {
        table.pushScope();
        visitBlock(main.body());
        table.popScope();
    }

    private void visitBlock(BlockNode b) {
        if (b == null) return;
        table.pushScope();
        for (VarDeclNode v : b.varDecls()) visitVarDecl(v);
        for (StatementNode s : b.statements()) visitStmt(s);
        table.popScope();
    }

    private void visitStmt(StatementNode s) {
        if (s instanceof AssignStmtNode n) {
            visitExpr(n.lvalue());
            visitExpr(n.value());
            DataTypeNode valueType = typeOfExpr(n.value());
            DataTypeNode lvalueType = typeOfLvalue(n.lvalue());
            if (lvalueType != null && "agent_list".equals(valueType.baseTypeName())) {
                String base = lvalueType.baseTypeName();
                if ("int".equals(base) || "float".equals(base) || "char".equals(base) || "string".equals(base) || "bool".equals(base) || "void".equals(base)) {
                    error(n.location(), "neighbors() returns a list of agents; declare the variable as an agent array (e.g. Drop[] drops;), not " + base);
                }
            }
            return;
        }
        if (s instanceof CallStmtNode n) {
            SymbolTable.Symbol sym = table.resolve(n.name());
            if (sym == null) error(n.location(), "Undefined function or procedure: " + n.name());
            else if (sym.kind != SymbolTable.Kind.FUNCTION) error(n.location(), "Not a function: " + n.name());
            else if (sym.paramTypes.size() != n.args().size()) error(n.location(), "Argument count mismatch for " + n.name() + ": expected " + sym.paramTypes.size() + ", got " + n.args().size());
            for (ExprNode a : n.args()) visitExpr(a);
            return;
        }
        if (s instanceof IfStmtNode n) {
            visitExpr(n.condition());
            visitStmt(n.thenBranch());
            if (n.elseBranch() != null) visitStmt(n.elseBranch());
            return;
        }
        if (s instanceof WhileStmtNode n) {
            visitExpr(n.condition());
            visitStmt(n.body());
            return;
        }
        if (s instanceof ForStmtNode n) {
            visitForInit(n.init());
            if (n.condition() != null) visitExpr(n.condition());
            for (AssignStmtNode a : n.update()) visitStmt(a);
            visitStmt(n.body());
            return;
        }
        if (s instanceof RepeatUntilStmtNode n) {
            visitBlock(n.block());
            visitExpr(n.condition());
            return;
        }
        if (s instanceof ReturnStmtNode n) {
            if (n.value() != null) visitExpr(n.value());
            return;
        }
        if (s instanceof AssertStmtNode n) { visitExpr(n.condition()); return; }
        if (s instanceof ReadStmtNode n) { visitExpr(n.lvalue()); return; }
        if (s instanceof PrintStmtNode n) { for (ExprNode a : n.args()) visitExpr(a); return; }
        if (s instanceof SpawnStmtNode n) {
            SymbolTable.Symbol sym = table.resolve(n.agentType());
            if (sym == null) error(n.location(), "Undefined agent type: " + n.agentType());
            else if (sym.kind != SymbolTable.Kind.AGENT) error(n.location(), "Not an agent type: " + n.agentType());
            for (ExprNode a : n.args()) visitExpr(a);
            return;
        }
        if (s instanceof MoveStmtNode n) {
            visitExpr(n.x());
            visitExpr(n.y());
            if (n.z() != null) visitExpr(n.z());
            return;
        }
        if (s instanceof StepStmtNode n) {
            if (n.arg() != null) visitExpr(n.arg());
            return;
        }
        if (s instanceof DestroyStmtNode n) {
            visitExpr(n.target());
            return;
        }
        if (s instanceof AbmCallStmtNode n) {
            for (ExprNode a : n.args()) visitExpr(a);
            return;
        }
        if (s instanceof BlockStmtNode n) { visitBlock(n.block()); return; }
        // BreakStmtNode, ContinueStmtNode, PlaceholderStmtNode: no children
    }

    private void visitForInit(ForInitNode init) {
        if (init instanceof ForInitVarDecl v) visitVarDecl(new VarDeclNode(v.dataType().location(), v.dataType(), v.declarators()));
        else if (init instanceof ForInitAssignList a) for (AssignStmtNode s : a.assignStmts()) visitStmt(s);
    }

    private void visitExpr(ExprNode e) {
        if (e instanceof IdentExprNode n) {
            if (table.resolve(n.name()) == null) error(n.location(), "Undefined identifier: " + n.name());
            return;
        }
        if (e instanceof SelfExprNode n) {
            if (agentDepth == 0) error(n.location(), "'self' is only valid inside an agent body");
            return;
        }
        if (e instanceof SelfFieldExprNode n) {
            if (agentDepth == 0) error(n.location(), "'self' is only valid inside an agent body");
            return;
        }
        if (e instanceof CallExprNode n) {
            SymbolTable.Symbol sym = table.resolve(n.name());
            if (sym == null) error(n.location(), "Undefined function: " + n.name());
            else if (sym.kind != SymbolTable.Kind.FUNCTION) error(n.location(), "Not a function: " + n.name());
            else if (sym.paramTypes.size() != n.args().size()) error(n.location(), "Argument count mismatch for " + n.name());
            for (ExprNode a : n.args()) visitExpr(a);
            return;
        }
        if (e instanceof LvalueExprNode n) {
            if (n.isSelfField() && agentDepth == 0) error(n.location(), "'self' is only valid inside an agent body");
            else if (!n.isSelfField() && table.resolve(n.baseName()) == null) error(n.location(), "Undefined identifier: " + n.baseName());
            return;
        }
        if (e instanceof BinaryExprNode n) {
            visitExpr(n.left());
            if (!".".equals(n.op())) visitExpr(n.right());
            return;
        }
        if (e instanceof UnaryExprNode n) { visitExpr(n.operand()); return; }
        if (e instanceof TernaryExprNode n) { visitExpr(n.condition()); visitExpr(n.thenExpr()); visitExpr(n.elseExpr()); return; }
        if (e instanceof ParenExprNode n) { visitExpr(n.inner()); return; }
        if (e instanceof AbmCallExprNode n) { for (ExprNode a : n.args()) visitExpr(a); return; }
        // LiteralExprNode, NullExprNode, PlaceholderExprNode: no resolution
    }

    private DataTypeNode typeOfExpr(ExprNode e) {
        if (e instanceof LiteralExprNode n) {
            Object v = n.value();
            if (v instanceof Integer) return new DataTypeNode(n.location(), "int", 0);
            if (v instanceof Float || v instanceof Double) return new DataTypeNode(n.location(), "float", 0);
            if (v instanceof Character) return new DataTypeNode(n.location(), "char", 0);
            if (v instanceof String) return new DataTypeNode(n.location(), "string", 0);
            if (v instanceof Boolean) return new DataTypeNode(n.location(), "bool", 0);
            return new DataTypeNode(n.location(), "int", 0);
        }
        if (e instanceof NullExprNode n) return new DataTypeNode(n.location(), "void", 1);
        if (e instanceof IdentExprNode n) {
            SymbolTable.Symbol s = table.resolve(n.name());
            return s != null && s.type != null ? s.type : new DataTypeNode(n.location(), "int", 0);
        }
        if (e instanceof SelfFieldExprNode n) return new DataTypeNode(n.location(), "int", 0);
        if (e instanceof CallExprNode n) {
            SymbolTable.Symbol s = table.resolve(n.name());
            return s != null && s.type != null ? s.type : new DataTypeNode(n.location(), "void", 0);
        }
        if (e instanceof BinaryExprNode bn) return typeOfExpr(bn.left());
        if (e instanceof UnaryExprNode un) return typeOfExpr(un.operand());
        if (e instanceof ParenExprNode p) return typeOfExpr(p.inner());
        if (e instanceof TernaryExprNode t) return typeOfExpr(t.thenExpr());
        if (e instanceof AbmCallExprNode n) {
            if ("neighbors".equals(n.name())) return new DataTypeNode(n.location(), "agent_list", 0);
            if ("rand".equals(n.name())) {
                if (n.args().size() > 2) error(n.location(), "rand expects 0, 1, or 2 arguments but got " + n.args().size());
                return new DataTypeNode(n.location(), "int", 0);
            }
        }
        return new DataTypeNode(e.location(), "int", 0);
    }

    /** Type of the variable or storage denoted by an lvalue; null if not a simple name. */
    private DataTypeNode typeOfLvalue(ExprNode e) {
        if (e instanceof IdentExprNode n) {
            SymbolTable.Symbol s = table.resolve(n.name());
            return s != null ? s.type : null;
        }
        if (e instanceof LvalueExprNode n) {
            SymbolTable.Symbol s = table.resolve(n.baseName());
            return s != null ? s.type : null;
        }
        if (e instanceof SelfFieldExprNode n) {
            return new DataTypeNode(n.location(), "int", 0);
        }
        return null;
    }

    private void error(SourceSpan loc, String message) {
        errors.add(new SemanticError(loc.line(), loc.col(), message));
    }
}
