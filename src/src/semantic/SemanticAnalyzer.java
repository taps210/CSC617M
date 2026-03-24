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
    private final List<SymbolEntry> symbolLog = new ArrayList<>();
    private final SymbolTable table = new SymbolTable();
    private int agentDepth = 0;
    /** agentName → (methodName → Symbol) — for resolving MethodCallExprNode. */
    private final java.util.Map<String, java.util.Map<String, SymbolTable.Symbol>> agentMethodTable = new java.util.HashMap<>();
    /** agentName → (fieldName → DataTypeNode) — for resolving field access on pointers. */
    private final java.util.Map<String, java.util.Map<String, DataTypeNode>> agentFieldTable = new java.util.HashMap<>();
    private DataTypeNode currentReturnType = null;
    private boolean logEnabled = false;

    public static List<SemanticError> analyze(ProgramNode program) {
        return analyzeDetailed(program).errors();
    }

    public static SemanticResult analyzeDetailed(ProgramNode program) {
        return new SemanticAnalyzer().analyzeInternal(program);
    }

    private static final java.util.Set<String> PRIMITIVE_TYPES =
            java.util.Set.of("int", "float", "char", "string", "bool", "void");

    private SemanticResult analyzeInternal(ProgramNode program) {
        errors.clear();
        symbolLog.clear();
        table.setOnDefine(sym -> {
            if (!logEnabled) return;
            symbolLog.add(new SymbolEntry(table.currentScopeName(), sym.name, sym.kind,
                    formatSymbolType(sym), sym.declarationSpan != null ? sym.declarationSpan.line() : 0));
        });
        table.pushScope("global");
        defineBuiltinTypes();
        logEnabled = true; // start logging after builtins
        // Pre-register all agent and world names so forward references resolve correctly
        for (TypeDeclNode td : program.typeDecls()) preRegisterTypeDecl(td);
        // Pre-register all function signatures so agents/world bodies can call them
        for (FuncDeclNode f : program.funcDecls()) preRegisterFuncDecl(f);
        for (TypeDeclNode td : program.typeDecls()) visitTypeDecl(td);
        for (ConstDeclNode c : program.constDecls()) visitConstDecl(c);
        for (VarDeclNode v : program.globalVarDecls()) visitVarDecl(v);
        for (FuncDeclNode f : program.funcDecls()) visitFuncDecl(f);
        checkMain(program.main());
        if (program.main() != null) visitMain(program.main());
        table.popScope();
        return new SemanticResult(List.copyOf(errors), List.copyOf(symbolLog));
    }

    private String formatSymbolType(SymbolTable.Symbol sym) {
        return switch (sym.kind) {
            case FUNCTION -> {
                String ret = sym.type != null ? sym.type.baseTypeName() : "void";
                String params = sym.paramTypes.stream()
                        .map(p -> p.baseTypeName() + "[]".repeat(p.pointerLevel()))
                        .collect(java.util.stream.Collectors.joining(", "));
                yield ret + "(" + params + ")";
            }
            case AGENT  -> "agent";
            case WORLD  -> "world";
            case TYPE   -> "type";
            default     -> sym.type != null
                    ? sym.type.baseTypeName() + "[]".repeat(sym.type.pointerLevel())
                    : "?";
        };
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

    /** Pass 1b: register function signatures so agent/world bodies can call them before funcDecl bodies are validated. */
    private void preRegisterFuncDecl(FuncDeclNode n) {
        if (table.definedInCurrentScope(n.name())) {
            error(n.location(), "Duplicate function: " + n.name());
            return;
        }
        List<DataTypeNode> paramTypes = n.params().stream().map(ParamNode::dataType).toList();
        table.define(SymbolTable.Symbol.function(n.name(), n.returnType(), paramTypes, n.location()));
    }

    /** Pass 1: register agent and world names so forward references resolve during body validation. */
    private void preRegisterTypeDecl(TypeDeclNode n) {
        if (n instanceof AgentDeclNode a) {
            if (!table.define(SymbolTable.Symbol.agent(a.name(), a.location())))
                error(a.location(), "Duplicate agent name: " + a.name());
        } else if (n instanceof WorldDeclNode w) {
            if (!table.define(SymbolTable.Symbol.world(w.name(), w.location())))
                error(w.location(), "Duplicate world name: " + w.name());
            // Register world fields in global scope so agents can read/write them from update/zone.
            // Suppress logging here — they are logged properly under "world:<name>" in visitTypeDecl.
            boolean savedLog = logEnabled;
            logEnabled = false;
            for (VarDeclNode f : w.fields()) {
                for (DeclaratorNode d : f.declarators()) {
                    table.define(SymbolTable.Symbol.variable(d.name(), f.dataType(), d.location()));
                }
            }
            logEnabled = savedLog;
        }
    }

    /** Pass 2: validate bodies (names already registered by preRegisterTypeDecl). */
    private void visitTypeDecl(TypeDeclNode n) {
        if (n instanceof TypeAliasNode a) {
            SymbolTable.Symbol sym = SymbolTable.Symbol.type(a.typeName(), null, a.location());
            if (!table.define(sym)) error(a.location(), "Duplicate type name: " + a.typeName());
            return;
        }
        if (n instanceof AgentDeclNode a) {
            // Name already registered in pre-pass; just validate body
            table.pushScope("agent:" + a.name());
            agentDepth++;
            // Cache agent fields for pointer field access resolution
            java.util.Map<String, DataTypeNode> fieldMap = new java.util.HashMap<>();
            for (VarDeclNode f : a.fields()) {
                visitVarDecl(f);
                // Also cache each field's type for resolveFieldOnAgent
                for (DeclaratorNode d : f.declarators()) {
                    int pointerLevel = f.dataType().pointerLevel() + d.arrayDims().size();
                    DataTypeNode fullType = new DataTypeNode(f.dataType().location(), f.dataType().baseTypeName(), pointerLevel, f.dataType().isPointer());
                    fieldMap.put(d.name(), fullType);
                }
            }
            agentFieldTable.put(a.name(), fieldMap);
            // Register method signatures in agent scope so the update block and other methods can call them
            java.util.Map<String, SymbolTable.Symbol> methodMap = new java.util.HashMap<>();
            for (FuncDeclNode m : a.methods()) {
                if (table.definedInCurrentScope(m.name())) {
                    error(m.location(), "Duplicate method '" + m.name() + "' in agent " + a.name());
                } else {
                    List<DataTypeNode> paramTypes = m.params().stream().map(ParamNode::dataType).toList();
                    SymbolTable.Symbol sym = SymbolTable.Symbol.function(m.name(), m.returnType(), paramTypes, m.location());
                    table.define(sym);
                    methodMap.put(m.name(), sym);
                }
            }
            agentMethodTable.put(a.name(), methodMap);
            for (ZoneDeclNode z : a.zones()) visitZoneDecl(z);
            visitBlock(a.updateBlock());
            // Validate method bodies (agent fields + other methods are in scope)
            for (FuncDeclNode m : a.methods()) visitAgentMethod(m);
            agentDepth--;
            table.popScope();
            return;
        }
        if (n instanceof WorldDeclNode w) {
            // Name already registered in pre-pass; just validate body
            table.pushScope("world:" + w.name());
            for (VarDeclNode f : w.fields()) visitVarDecl(f);
            if (w.preBlock() != null) visitBlock(w.preBlock());
            if (w.postBlock() != null) visitBlock(w.postBlock());
            table.popScope();
        }
    }

    private void visitZoneDecl(ZoneDeclNode z) {
        visitExpr(z.condition());
        SymbolTable.Symbol targetSym = table.resolve(z.targetIdent());
        if (targetSym == null) error(z.location(), "Undefined agent type in zone: " + z.targetIdent());
        else if (targetSym.kind != SymbolTable.Kind.AGENT) error(z.location(), "Not an agent type in zone: " + z.targetIdent());
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
        String base = n.dataType().baseTypeName();
        if (!PRIMITIVE_TYPES.contains(base)) {
            SymbolTable.Symbol typeSym = table.resolve(base);
            if (typeSym == null) error(n.location(), "Undefined type: " + base);
            else if (typeSym.kind != SymbolTable.Kind.AGENT && typeSym.kind != SymbolTable.Kind.TYPE)
                error(n.location(), "Not a valid type: " + base);
        }
        for (DeclaratorNode d : n.declarators()) {
            if (table.definedInCurrentScope(d.name())) error(d.location(), "Duplicate variable: " + d.name());
            else {
                // Combine base dataType with declarator's arrayDims to get full type
                int pointerLevel = n.dataType().pointerLevel() + d.arrayDims().size();
                boolean isPointer = n.dataType().isPointer();
                DataTypeNode fullType = new DataTypeNode(n.dataType().location(), n.dataType().baseTypeName(), pointerLevel, isPointer);
                table.define(SymbolTable.Symbol.variable(d.name(), fullType, d.location()));
            }
            if (d.init() != null) visitExpr(d.init());
        }
    }

    /** Validates an agent method body. Called while agent scope is still on the stack. */
    private void visitAgentMethod(FuncDeclNode n) {
        table.pushScope("func:" + n.name());
        for (ParamNode p : n.params()) {
            if (table.definedInCurrentScope(p.name())) error(p.location(), "Duplicate parameter: " + p.name());
            else {
                // Combine base dataType with param's arrayDims to get full type
                int pointerLevel = p.dataType().pointerLevel() + p.arrayDims().size();
                boolean isPointer = p.dataType().isPointer();
                DataTypeNode fullType = new DataTypeNode(p.dataType().location(), p.dataType().baseTypeName(), pointerLevel, isPointer);
                table.define(SymbolTable.Symbol.variable(p.name(), fullType, p.location()));
            }
        }
        DataTypeNode prevReturn = currentReturnType;
        currentReturnType = n.returnType();
        visitBlock(n.body());
        currentReturnType = prevReturn;
        table.popScope();
    }

    private void visitFuncDecl(FuncDeclNode n) {
        // Signature already registered by preRegisterFuncDecl; duplicate errors reported there.
        // Only validate the body here.
        if (!table.definedInCurrentScope(n.name())) {
            // Fallback: wasn't pre-registered (e.g. duplicate was skipped), nothing to validate.
            return;
        }
        table.pushScope("func:" + n.name());
        for (ParamNode p : n.params()) {
            if (table.definedInCurrentScope(p.name())) error(p.location(), "Duplicate parameter: " + p.name());
            else {
                // Combine base dataType with param's arrayDims to get full type
                int pointerLevel = p.dataType().pointerLevel() + p.arrayDims().size();
                boolean isPointer = p.dataType().isPointer();
                DataTypeNode fullType = new DataTypeNode(p.dataType().location(), p.dataType().baseTypeName(), pointerLevel, isPointer);
                table.define(SymbolTable.Symbol.variable(p.name(), fullType, p.location()));
            }
        }
        DataTypeNode prevReturn = currentReturnType;
        currentReturnType = n.returnType();
        visitBlock(n.body());
        currentReturnType = prevReturn;
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
        table.pushScope("main");
        DataTypeNode prevReturn = currentReturnType;
        currentReturnType = new DataTypeNode(main.location(), "void", 0);
        visitBlock(main.body());
        currentReturnType = prevReturn;
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

            // Disallow reassigning constants (rubric: const reassignment)
            DataTypeNode lvalueType = typeOfLvalue(n.lvalue());
            if (n.lvalue() instanceof IdentExprNode id) {
                SymbolTable.Symbol sym = table.resolve(id.name());
                if (sym != null && sym.kind == SymbolTable.Kind.CONSTANT) {
                    error(n.location(), "Constant reassignment: " + id.name());
                }
            } else if (n.lvalue() instanceof LvalueExprNode lv && !lv.isSelfField()) {
                SymbolTable.Symbol sym = table.resolve(lv.baseName());
                if (sym != null && sym.kind == SymbolTable.Kind.CONSTANT) {
                    error(n.location(), "Constant reassignment: " + lv.baseName());
                }
            }

            DataTypeNode valueType = typeOfExpr(n.value());

            // Type checking for assignments (rubric: type mismatch)
            if (lvalueType != null && valueType != null && !typesCompatible(lvalueType, valueType)) {
                error(n.location(), "Type mismatch in assignment: cannot assign "
                        + formatType(valueType) + " to " + formatType(lvalueType));
            }

            // Better error message for agent_list assigned to primitives
            if (lvalueType != null && "agent_list".equals(valueType.baseTypeName())) {
                String base = lvalueType.baseTypeName();
                if ("int".equals(base) || "float".equals(base) || "char".equals(base) || "string".equals(base) || "bool".equals(base) || "void".equals(base)) {
                    error(n.location(), "neighbors() returns a list of agents; declare the variable as an agent array (e.g. Wolf[] nearby;), not " + base);
                }
            }
            return;
        }
        if (s instanceof CallStmtNode n) {
            SymbolTable.Symbol sym = table.resolve(n.name());
            if (sym == null) error(n.location(), "Undefined function or procedure: " + n.name());
            else if (sym.kind != SymbolTable.Kind.FUNCTION) error(n.location(), "Not a function: " + n.name());
            else if (sym.paramTypes.size() != n.args().size()) error(n.location(), "Argument count mismatch for " + n.name() + ": expected " + sym.paramTypes.size() + ", got " + n.args().size());
            for (int i = 0; i < n.args().size(); i++) {
                ExprNode a = n.args().get(i);
                visitExpr(a);
                if (sym != null && sym.kind == SymbolTable.Kind.FUNCTION && i < sym.paramTypes.size()) {
                    DataTypeNode actual = typeOfExpr(a);
                    DataTypeNode expected = sym.paramTypes.get(i);
                    if (!typesCompatible(expected, actual)) {
                        error(a.location(), "Argument type mismatch for " + n.name() + " param #" + (i + 1)
                                + ": expected " + formatType(expected) + ", got " + formatType(actual));
                    }
                }
            }
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
            checkReturnType(n);
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
            if (agentDepth == 0) error(n.location(), "move() is only valid inside an agent body");
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
            if (agentDepth == 0) error(n.location(), "destroy() is only valid inside an agent body");
            visitExpr(n.target());
            return;
        }
        if (s instanceof AbmCallStmtNode n) {
            if ("neighbors".equals(n.name()) && n.args().size() > 2)
                error(n.location(), "neighbors expects 0, 1, or 2 arguments but got " + n.args().size());
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
            else {
                // Validate that the field exists in the current agent scope
                SymbolTable.Symbol s = table.resolve(n.fieldName());
                if (s == null) error(n.location(), "Undefined identifier: " + n.fieldName());
            }
            return;
        }
        if (e instanceof NewExprNode n) {
            // Validate type exists as AGENT (already checked in typeOfExpr)
            SymbolTable.Symbol sym = table.resolve(n.typeName());
            if (sym == null || sym.kind != SymbolTable.Kind.AGENT) {
                error(n.location(), "Undefined or non-agent type: " + n.typeName());
            }
            return;
        }
        if (e instanceof CallExprNode n) {
            SymbolTable.Symbol sym = table.resolve(n.name());
            if (sym == null) error(n.location(), "Undefined function: " + n.name());
            else if (sym.kind != SymbolTable.Kind.FUNCTION) error(n.location(), "Not a function: " + n.name());
            else if (sym.paramTypes.size() != n.args().size()) error(n.location(), "Argument count mismatch for " + n.name());
            for (int i = 0; i < n.args().size(); i++) {
                ExprNode a = n.args().get(i);
                visitExpr(a);
                if (sym != null && sym.kind == SymbolTable.Kind.FUNCTION && i < sym.paramTypes.size()) {
                    DataTypeNode actual = typeOfExpr(a);
                    DataTypeNode expected = sym.paramTypes.get(i);
                    if (!typesCompatible(expected, actual)) {
                        error(a.location(), "Argument type mismatch for " + n.name() + " param #" + (i + 1)
                                + ": expected " + formatType(expected) + ", got " + formatType(actual));
                    }
                }
            }
            return;
        }
        if (e instanceof MethodCallExprNode n) {
            visitExpr(n.target());
            DataTypeNode targetType = typeOfExpr(n.target());
            java.util.Map<String, SymbolTable.Symbol> methods = agentMethodTable.get(targetType.baseTypeName());
            if (methods == null) {
                error(n.location(), "Type '" + targetType.baseTypeName() + "' has no methods");
            } else {
                SymbolTable.Symbol sym = methods.get(n.methodName());
                if (sym == null) error(n.location(), "Undefined method '" + n.methodName() + "' on " + targetType.baseTypeName());
                else if (sym.paramTypes.size() != n.args().size()) error(n.location(), "Argument count mismatch for method " + n.methodName());
                else {
                    for (int i = 0; i < n.args().size() && sym != null; i++) {
                        DataTypeNode actual = typeOfExpr(n.args().get(i));
                        DataTypeNode expected = sym.paramTypes.get(i);
                        if (!typesCompatible(expected, actual)) {
                            error(n.args().get(i).location(), "Argument type mismatch for method " + n.methodName() + " param #" + (i + 1)
                                    + ": expected " + formatType(expected) + ", got " + formatType(actual));
                        }
                    }
                }
            }
            for (ExprNode a : n.args()) visitExpr(a);
            return;
        }
        if (e instanceof LvalueExprNode n) {
            if (n.isSelfField()) {
                if (agentDepth == 0) error(n.location(), "'self' is only valid inside an agent body");
                else if (table.resolve(n.baseName()) == null) error(n.location(), "Undefined identifier: " + n.baseName());
            } else if (table.resolve(n.baseName()) == null) {
                error(n.location(), "Undefined identifier: " + n.baseName());
            }
            return;
        }
        if (e instanceof BinaryExprNode n) {
            visitExpr(n.left());
            if (!".".equals(n.op()) && !"->".equals(n.op())) visitExpr(n.right());
            return;
        }
        if (e instanceof UnaryExprNode n) { visitExpr(n.operand()); return; }
        if (e instanceof TernaryExprNode n) { visitExpr(n.condition()); visitExpr(n.thenExpr()); visitExpr(n.elseExpr()); return; }
        if (e instanceof ParenExprNode n) { visitExpr(n.inner()); return; }
        if (e instanceof AbmCallExprNode n) {
            if ("neighbors".equals(n.name()) && n.args().size() > 2)
                error(n.location(), "neighbors expects 0, 1, or 2 arguments but got " + n.args().size());
            for (ExprNode a : n.args()) visitExpr(a);
            return;
        }
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
        if (e instanceof NullExprNode n) return new DataTypeNode(n.location(), "void", 1, true);
        if (e instanceof IdentExprNode n) {
            SymbolTable.Symbol s = table.resolve(n.name());
            return s != null && s.type != null ? s.type : new DataTypeNode(n.location(), "int", 0);
        }
        if (e instanceof SelfFieldExprNode n) {
            SymbolTable.Symbol s = table.resolve(n.fieldName());
            return s != null && s.type != null ? s.type : new DataTypeNode(n.location(), "int", 0);
        }
        if (e instanceof MethodCallExprNode n) {
            DataTypeNode targetType = typeOfExpr(n.target());
            java.util.Map<String, SymbolTable.Symbol> methods = agentMethodTable.get(targetType.baseTypeName());
            if (methods != null) {
                SymbolTable.Symbol sym = methods.get(n.methodName());
                if (sym != null && sym.type != null) return sym.type;
            }
            return new DataTypeNode(n.location(), "void", 0);
        }
        if (e instanceof CallExprNode n) {
            SymbolTable.Symbol s = table.resolve(n.name());
            return s != null && s.type != null ? s.type : new DataTypeNode(n.location(), "void", 0);
        }
        if (e instanceof BinaryExprNode bn) {
            switch (bn.op()) {
                case ">", "<", ">=", "<=", "==", "!=", "&&", "||":
                    return new DataTypeNode(bn.location(), "bool", 0);
                case "[]":
                    // Array element access: reduce pointer level by 1
                    DataTypeNode arrType = typeOfExpr(bn.left());
                    int newLevel = Math.max(0, arrType.pointerLevel() - 1);
                    return new DataTypeNode(bn.location(), arrType.baseTypeName(), newLevel);
                case "->":
                    // Field access through pointer: ptr->field
                    DataTypeNode ptrType = typeOfExpr(bn.left());
                    if (!ptrType.isPointer()) {
                        error(bn.location(), "'->' requires a pointer type; use '.' for direct access");
                        return new DataTypeNode(bn.location(), "int", 0);
                    }
                    // Resolve field on the pointed-to agent type
                    String baseType = ptrType.baseTypeName();
                    String fieldName = bn.right() instanceof IdentExprNode ? ((IdentExprNode) bn.right()).name() : "";
                    DataTypeNode fieldType = resolveFieldOnAgent(baseType, fieldName);
                    return fieldType != null ? fieldType : new DataTypeNode(bn.location(), "int", 0);
            }
            return typeOfExpr(bn.left());
        }
        if (e instanceof UnaryExprNode un) return typeOfExpr(un.operand());
        if (e instanceof ParenExprNode p) return typeOfExpr(p.inner());
        if (e instanceof TernaryExprNode t) return typeOfExpr(t.thenExpr());
        if (e instanceof NewExprNode n) {
            // Validate type exists as AGENT
            SymbolTable.Symbol sym = table.resolve(n.typeName());
            if (sym == null || sym.kind != SymbolTable.Kind.AGENT) {
                error(n.location(), "Undefined or non-agent type: " + n.typeName());
                return new DataTypeNode(n.location(), "int", 0);
            }
            // new returns a pointer to the agent type
            return new DataTypeNode(n.location(), n.typeName(), 1, true);
        }
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
            SymbolTable.Symbol s = table.resolve(n.fieldName());
            return s != null && s.type != null ? s.type : new DataTypeNode(n.location(), "int", 0);
        }
        if (e instanceof BinaryExprNode bn && "->".equals(bn.op())) {
            // Field assignment through pointer: ptr->field = value
            DataTypeNode ptrType = typeOfExpr(bn.left());
            if (!ptrType.isPointer()) {
                error(bn.location(), "'->' requires a pointer type");
                return null;
            }
            String baseType = ptrType.baseTypeName();
            String fieldName = bn.right() instanceof IdentExprNode ? ((IdentExprNode) bn.right()).name() : "";
            return resolveFieldOnAgent(baseType, fieldName);
        }
        return null;
    }

    private void checkReturnType(ReturnStmtNode n) {
        boolean isVoidContext = currentReturnType == null || "void".equals(currentReturnType.baseTypeName());
        if (n.value() == null) {
            if (!isVoidContext)
                error(n.location(), "Missing return value in function returning " + currentReturnType.baseTypeName());
        } else {
            if (isVoidContext) {
                error(n.location(), "void function must not return a value");
            } else {
                DataTypeNode actual = typeOfExpr(n.value());
                if (!typesCompatible(currentReturnType, actual))
                    error(n.location(), "Return type mismatch: expected " + formatType(currentReturnType)
                            + " but got " + formatType(actual));
            }
        }
    }

    private static String formatType(DataTypeNode t) {
        if (t == null) return "?";
        return t.baseTypeName() + "*".repeat(Math.max(0, t.pointerLevel()));
    }

    /**
     * Type compatibility:
     * - Exact match (including pointerLevel and isPointer flag)
     * - Numeric widening: int/float compatible when both are non-pointers
     * - null (void*) is assignable to any pointer type
     * - Pointer types (isPointer=true) not compatible with array types (isPointer=false)
     */
    private static boolean typesCompatible(DataTypeNode expected, DataTypeNode actual) {
        if (expected == null || actual == null) return true;

        // Special case: agent_list is compatible with any agent array type
        if ("agent_list".equals(actual.baseTypeName())) {
            // agent_list should only be assigned to agent arrays, not primitives
            // The expected type's pointer level should be 1 (array) and base type should be a known agent
            return expected.pointerLevel() > 0 && !expected.isPointer();
        }

        // Pointers and arrays are distinguished by isPointer flag
        if (expected.isPointer() != actual.isPointer()) {
            // allow null (void*) to any pointer type
            if (expected.isPointer() && actual.isPointer() && expected.pointerLevel() > 0 &&
                actual.pointerLevel() > 0 && "void".equals(actual.baseTypeName())) return true;
            return false;
        }

        if (expected.pointerLevel() != actual.pointerLevel()) {
            // allow null to any pointer type
            if (expected.isPointer() && actual.isPointer() && expected.pointerLevel() > 0 &&
                actual.pointerLevel() > 0 && "void".equals(actual.baseTypeName())) return true;
            return false;
        }
        if (expected.baseTypeName().equals(actual.baseTypeName())) return true;
        // Allow null (void*) to be assigned to any pointer type
        if (expected.isPointer() && actual.isPointer() && "void".equals(actual.baseTypeName())) return true;
        if (expected.pointerLevel() > 0) return false;
        boolean expNum = "int".equals(expected.baseTypeName()) || "float".equals(expected.baseTypeName());
        boolean actNum = "int".equals(actual.baseTypeName()) || "float".equals(actual.baseTypeName());
        return expNum && actNum;
    }

    /**
     * Resolve a field on an agent type by name.
     * Returns the DataTypeNode for the field, or null if not found.
     */
    private DataTypeNode resolveFieldOnAgent(String agentName, String fieldName) {
        java.util.Map<String, DataTypeNode> fields = agentFieldTable.get(agentName);
        if (fields == null) return null;
        return fields.get(fieldName);
    }

    private void error(SourceSpan loc, String message) {
        errors.add(new SemanticError(loc.line(), loc.col(), message));
    }
}
