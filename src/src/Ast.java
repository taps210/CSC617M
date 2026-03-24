package src;

import java.util.List;

/**
 * All AST node types in one place. Use Ast.ProgramNode, Ast.SourceSpan, etc.,
 * or "import static src.Ast.*" to use short names.
 */
public final class Ast {

    private Ast() {}

    // --- Source location ---
    public record SourceSpan(int line, int col) {
        public static SourceSpan of(int line, int col) {
            return new SourceSpan(line, col);
        }
    }

    // --- Expression hierarchy ---
    public sealed interface ExprNode
            permits BinaryExprNode, UnaryExprNode, CallExprNode, MethodCallExprNode, LiteralExprNode,
            IdentExprNode, SelfFieldExprNode, SelfExprNode, NullExprNode, ParenExprNode,
            LvalueExprNode, AbmCallExprNode, PlaceholderExprNode, TernaryExprNode, NewExprNode {
        SourceSpan location();
    }

    public record BinaryExprNode(SourceSpan location, ExprNode left, String op, ExprNode right) implements ExprNode {}
    public record UnaryExprNode(SourceSpan location, String op, ExprNode operand) implements ExprNode {}
    public record CallExprNode(SourceSpan location, String name, List<ExprNode> args) implements ExprNode {}
    /** Call a method on an agent reference: target.methodName(args). */
    public record MethodCallExprNode(SourceSpan location, ExprNode target, String methodName, List<ExprNode> args) implements ExprNode {}
    public record LiteralExprNode(SourceSpan location, Object value) implements ExprNode {}
    public record IdentExprNode(SourceSpan location, String name) implements ExprNode {}
    public record SelfFieldExprNode(SourceSpan location, String fieldName) implements ExprNode {}
    public record SelfExprNode(SourceSpan location) implements ExprNode {}
    public record NullExprNode(SourceSpan location) implements ExprNode {}
    public record ParenExprNode(SourceSpan location, ExprNode inner) implements ExprNode {}
    public record LvalueExprNode(SourceSpan location, String baseName, boolean isSelfField) implements ExprNode {}
    public record AbmCallExprNode(SourceSpan location, String name, List<ExprNode> args) implements ExprNode {}
    public record PlaceholderExprNode(SourceSpan location) implements ExprNode {}
    public record TernaryExprNode(SourceSpan location, ExprNode condition, ExprNode thenExpr, ExprNode elseExpr) implements ExprNode {}
    public record NewExprNode(SourceSpan location, String typeName) implements ExprNode {}

    // --- Statement hierarchy ---
    public sealed interface StatementNode
            permits AssignStmtNode, CallStmtNode, IfStmtNode, WhileStmtNode, ForStmtNode,
            RepeatUntilStmtNode, ReturnStmtNode, BreakStmtNode, ContinueStmtNode, AssertStmtNode,
            ReadStmtNode, PrintStmtNode, SpawnStmtNode, MoveStmtNode, StepStmtNode, DestroyStmtNode,
            AbmCallStmtNode, BlockStmtNode, PlaceholderStmtNode {
        SourceSpan location();
    }

    // --- Types used by both decls and statements ---
    public record DataTypeNode(SourceSpan location, String baseTypeName, int pointerLevel, boolean isPointer) {
        // Backward-compatible 3-arg constructor: defaults isPointer to false (for arrays and non-pointer types)
        public DataTypeNode(SourceSpan location, String baseTypeName, int pointerLevel) {
            this(location, baseTypeName, pointerLevel, false);
        }
    }
    public record DeclaratorNode(SourceSpan location, String name, List<Integer> arrayDims, ExprNode init) {}
    public record VarDeclNode(SourceSpan location, DataTypeNode dataType, List<DeclaratorNode> declarators) {}
    public record BlockNode(SourceSpan location, List<VarDeclNode> varDecls, List<StatementNode> statements) {}
    public record ParamNode(SourceSpan location, DataTypeNode dataType, String name, List<Integer> arrayDims) {}

    // --- For-init ---
    public sealed interface ForInitNode permits ForInitVarDecl, ForInitAssignList {}
    public record ForInitVarDecl(DataTypeNode dataType, List<DeclaratorNode> declarators) implements ForInitNode {}
    public record ForInitAssignList(List<AssignStmtNode> assignStmts) implements ForInitNode {}

    // --- Statement implementations (AssignStmtNode before ForStmtNode) ---
    public record AssignStmtNode(SourceSpan location, ExprNode lvalue, ExprNode value) implements StatementNode {}
    public record CallStmtNode(SourceSpan location, String name, List<ExprNode> args) implements StatementNode {}
    public record IfStmtNode(SourceSpan location, ExprNode condition, StatementNode thenBranch, StatementNode elseBranch) implements StatementNode {}
    public record WhileStmtNode(SourceSpan location, ExprNode condition, StatementNode body) implements StatementNode {}
    public record ForStmtNode(SourceSpan location, ForInitNode init, ExprNode condition, List<AssignStmtNode> update, StatementNode body) implements StatementNode {}
    public record RepeatUntilStmtNode(SourceSpan location, BlockNode block, ExprNode condition) implements StatementNode {}
    public record ReturnStmtNode(SourceSpan location, ExprNode value) implements StatementNode {}
    public record BreakStmtNode(SourceSpan location) implements StatementNode {}
    public record ContinueStmtNode(SourceSpan location) implements StatementNode {}
    public record AssertStmtNode(SourceSpan location, ExprNode condition) implements StatementNode {}
    public record ReadStmtNode(SourceSpan location, ExprNode lvalue) implements StatementNode {}
    public record PrintStmtNode(SourceSpan location, List<ExprNode> args) implements StatementNode {}
    public record SpawnStmtNode(SourceSpan location, String agentType, List<ExprNode> args) implements StatementNode {}
    public record MoveStmtNode(SourceSpan location, ExprNode x, ExprNode y, ExprNode z) implements StatementNode {}
    public record StepStmtNode(SourceSpan location, ExprNode arg) implements StatementNode {}
    public record DestroyStmtNode(SourceSpan location, ExprNode target) implements StatementNode {}
    public record AbmCallStmtNode(SourceSpan location, String name, List<ExprNode> args) implements StatementNode {}
    public record BlockStmtNode(SourceSpan location, BlockNode block) implements StatementNode {}
    public record PlaceholderStmtNode(SourceSpan location) implements StatementNode {}

    // --- Declarations and program ---
    public record RecordTypeNode(SourceSpan location, List<VarDeclNode> fields) {}
    public record UseNode(SourceSpan location, List<String> filenameSegments) {}
    public record ConstDeclNode(SourceSpan location, String name, ExprNode value) {}
    public record MainFunctionNode(SourceSpan location, BlockNode body) {}

    public sealed interface TypeDeclNode permits TypeAliasNode, AgentDeclNode, WorldDeclNode {}
    public record TypeAliasNode(SourceSpan location, String typeName, RecordTypeNode recordType) implements TypeDeclNode {}
    public record ZoneDeclNode(SourceSpan location, String name, ExprNode condition, String targetIdent, BlockNode block) {}
    public record AgentDeclNode(SourceSpan location, String name, List<VarDeclNode> fields, List<ZoneDeclNode> zones, BlockNode updateBlock, List<FuncDeclNode> methods) implements TypeDeclNode {}
    public record WorldDeclNode(SourceSpan location, String name, List<VarDeclNode> fields, BlockNode preBlock, BlockNode postBlock) implements TypeDeclNode {}

    public record FuncDeclNode(SourceSpan location, String name, DataTypeNode returnType, List<ParamNode> params, BlockNode body) {}
    public record ProgramNode(SourceSpan location, List<UseNode> uses, List<TypeDeclNode> typeDecls, List<ConstDeclNode> constDecls, List<VarDeclNode> globalVarDecls, List<FuncDeclNode> funcDecls, MainFunctionNode main) {}
}
