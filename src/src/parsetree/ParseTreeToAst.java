package src.parsetree;

import src.Token;
import src.TokenType;
import static src.Ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts a parse tree (root PROGRAM) to the AST (ProgramNode and all other AST types).
 */
public final class ParseTreeToAst {

    public static ProgramNode convert(ParseTreeNode root) {
        if (root.kind() != ParseTreeKind.PROGRAM) throw new IllegalArgumentException("Root must be PROGRAM");
        List<ParseTreeNode> c = root.children();
        return new ProgramNode(
                span(root),
                toUseList(child(c, 0)),
                toTypeDeclList(child(c, 1)),
                toConstDeclList(child(c, 2)),
                toVarDeclList(child(c, 3)),
                toFuncDeclList(child(c, 4)),
                toMain(child(c, 5))
        );
    }

    private static SourceSpan span(ParseTreeNode n) {
        return n.token() != null ? SourceSpan.of(n.token().line(), n.token().col()) : SourceSpan.of(1, 1);
    }

    private static ParseTreeNode child(List<ParseTreeNode> c, int i) {
        return i < c.size() ? c.get(i) : null;
    }

    private static List<UseNode> toUseList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.USE_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toUseStmt).collect(Collectors.toList());
    }

    private static UseNode toUseStmt(ParseTreeNode n) {
        ParseTreeNode filename = child(n.children(), 0);
        List<String> segments = filenameSegments(filename);
        return new UseNode(span(n), segments);
    }

    private static List<String> filenameSegments(ParseTreeNode filename) {
        if (filename == null || filename.kind() != ParseTreeKind.FILENAME) return List.of();
        List<String> out = new ArrayList<>();
        for (ParseTreeNode t : filename.children()) {
            if (t.isTerminal() && t.token().type() == TokenType.IDENT) out.add(t.token().lexeme());
        }
        return out;
    }

    private static List<TypeDeclNode> toTypeDeclList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.TYPE_DECL_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toTypeDecl).collect(Collectors.toList());
    }

    private static TypeDeclNode toTypeDecl(ParseTreeNode n) {
        if (n == null) return null;
        if (n.kind() == ParseTreeKind.TYPE_ALIAS_DECL) return toTypeAlias(n);
        if (n.kind() == ParseTreeKind.AGENT_DECL) return toAgentDecl(n);
        if (n.kind() == ParseTreeKind.WORLD_DECL) return toWorldDecl(n);
        return null;
    }

    private static TypeAliasNode toTypeAlias(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String name = identLexeme(c, 0);
        RecordTypeNode rt = toRecordType(child(c, 1));
        return new TypeAliasNode(span(n), name, rt);
    }

    private static RecordTypeNode toRecordType(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.RECORD_TYPE) return null;
        List<VarDeclNode> fields = toFieldDeclList(child(n.children(), 0));
        return new RecordTypeNode(span(n), fields);
    }

    private static List<VarDeclNode> toFieldDeclList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.FIELD_DECL_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toVarDecl).collect(Collectors.toList());
    }

    private static AgentDeclNode toAgentDecl(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String name = identLexeme(c, 0);
        List<VarDeclNode> fields = toFieldDeclList(child(c, 1));
        List<ZoneDeclNode> zones = toZoneDeclList(child(c, 2));
        BlockNode updateBlock = toBlock(child(c, 3));
        return new AgentDeclNode(span(n), name, fields, zones, updateBlock);
    }

    private static List<ZoneDeclNode> toZoneDeclList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.ZONE_DECL_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toZoneDecl).collect(Collectors.toList());
    }

    private static ZoneDeclNode toZoneDecl(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String name = identLexeme(c, 0);
        ExprNode condition = toExpr(child(c, 1));
        String target = identLexeme(c, 2);
        BlockNode block = toBlock(child(c, 3));
        return new ZoneDeclNode(span(n), name, condition, target, block);
    }

    private static WorldDeclNode toWorldDecl(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String name = identLexeme(c, 0);
        List<VarDeclNode> fields = toFieldDeclList(child(c, 1));
        BlockNode preBlock = null;
        BlockNode postBlock = null;
        int i = 2;
        if (i < c.size() && c.get(i).kind() == ParseTreeKind.BLOCK) { preBlock = toBlock(c.get(i)); i++; }
        if (i < c.size() && c.get(i).kind() == ParseTreeKind.BLOCK) { postBlock = toBlock(c.get(i)); }
        return new WorldDeclNode(span(n), name, fields, preBlock, postBlock);
    }

    private static String identLexeme(List<ParseTreeNode> c, int idx) {
        ParseTreeNode x = child(c, idx);
        if (x != null && x.isTerminal() && x.token().type() == TokenType.IDENT) return x.token().lexeme();
        return "";
    }

    private static List<ConstDeclNode> toConstDeclList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.CONST_DECL_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toConstDecl).collect(Collectors.toList());
    }

    private static ConstDeclNode toConstDecl(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String name = identLexeme(c, 0);
        ExprNode value = toExpr(child(c, 1));
        return new ConstDeclNode(span(n), name, value);
    }

    private static List<VarDeclNode> toVarDeclList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.GLOBAL_VAR_DECL_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toVarDecl).collect(Collectors.toList());
    }

    private static VarDeclNode toVarDecl(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.VAR_DECL) return null;
        List<ParseTreeNode> c = n.children();
        DataTypeNode dt = toDataType(child(c, 0));
        List<DeclaratorNode> decls = toDeclaratorList(child(c, 1));
        return new VarDeclNode(span(n), dt, decls);
    }

    private static DataTypeNode toDataType(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.DATA_TYPE) return new DataTypeNode(SourceSpan.of(0, 0), "int", 0);
        List<ParseTreeNode> c = n.children();
        String base = baseTypeFrom(child(c, 0));
        int stars = 0;
        for (int i = 1; i < c.size(); i++) if (c.get(i).isTerminal() && c.get(i).token().type() == TokenType.STAR) stars++;
        return new DataTypeNode(span(n), base, stars);
    }

    private static String baseTypeFrom(ParseTreeNode n) {
        if (n == null || !n.isTerminal()) return "int";
        Token t = n.token();
        return switch (t.type()) {
            case INT -> "int";
            case FLOAT -> "float";
            case CHAR -> "char";
            case STRING -> "string";
            case BOOL -> "bool";
            case IDENT -> t.lexeme();
            default -> "int";
        };
    }

    private static List<DeclaratorNode> toDeclaratorList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.DECLARATOR_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toDeclarator).collect(Collectors.toList());
    }

    private static DeclaratorNode toDeclarator(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.DECLARATOR) return null;
        List<ParseTreeNode> c = n.children();
        String name = identLexeme(c, 0);
        List<Integer> dims = new ArrayList<>();
        int i = 1;
        while (i < c.size()) {
            if (c.get(i).isTerminal() && c.get(i).token().type() == TokenType.INT_LIT) {
                Object lit = c.get(i).token().literal();
                dims.add(lit != null ? ((Number) lit).intValue() : 0);
                i++;
            } else if (c.get(i).isTerminal() && c.get(i).token().type() == TokenType.ASSIGN) {
                i++;
                ExprNode init = toExpr(child(c, i)); i++;
                return new DeclaratorNode(span(n), name, dims, init);
            } else i++;
        }
        return new DeclaratorNode(span(n), name, dims, null);
    }

    private static List<FuncDeclNode> toFuncDeclList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.FUNC_DECL_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toFuncDecl).collect(Collectors.toList());
    }

    private static FuncDeclNode toFuncDecl(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.FUNC_DECL) return null;
        List<ParseTreeNode> c = n.children();
        DataTypeNode returnType = toReturnType(child(c, 0));
        String name = identLexeme(c, 1);
        List<ParamNode> params = toParamList(child(c, 2));
        BlockNode body = toBlock(child(c, 3));
        return new FuncDeclNode(span(n), name, returnType, params, body);
    }

    private static DataTypeNode toReturnType(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.RETURN_TYPE) return null;
        if (n.children().stream().anyMatch(c -> c.isTerminal() && c.token().type() == TokenType.VOID)) return null;
        return toDataType(child(n.children(), 0));
    }

    private static List<ParamNode> toParamList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.PARAM_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toParam).collect(Collectors.toList());
    }

    private static ParamNode toParam(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.PARAM) return null;
        List<ParseTreeNode> c = n.children();
        DataTypeNode dt = toDataType(child(c, 0));
        String name = identLexeme(c, 1);
        List<Integer> dims = new ArrayList<>();
        for (int i = 2; i < c.size(); i++) {
            if (c.get(i).isTerminal() && c.get(i).token().type() == TokenType.INT_LIT) {
                Object lit = c.get(i).token().literal();
                dims.add(lit != null ? ((Number) lit).intValue() : 0);
            }
        }
        return new ParamNode(span(n), dt, name, dims);
    }

    private static MainFunctionNode toMain(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.MAIN_FUNCTION) return null;
        BlockNode body = toBlock(child(n.children(), 0));
        return new MainFunctionNode(span(n), body);
    }

    private static BlockNode toBlock(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.BLOCK) return new BlockNode(SourceSpan.of(0, 0), List.of(), List.of());
        List<ParseTreeNode> c = n.children();
        List<VarDeclNode> varDecls = new ArrayList<>();
        List<StatementNode> statements = new ArrayList<>();
        int i = 0;
        if (i < c.size() && c.get(i).kind() == ParseTreeKind.FIELD_DECL_LIST) {
            varDecls = toFieldDeclList(c.get(i));
            i++;
        }
        while (i < c.size() && c.get(i).kind() == ParseTreeKind.STATEMENT) {
            statements.add(toStatement(c.get(i)));
            i++;
        }
        return new BlockNode(span(n), varDecls, statements);
    }

    private static StatementNode toStatement(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.STATEMENT) return new PlaceholderStmtNode(SourceSpan.of(0, 0));
        ParseTreeNode inner = child(n.children(), 0);
        if (inner == null) return new PlaceholderStmtNode(span(n));
        return switch (inner.kind()) {
            case ASSIGN_STMT -> toAssignStmt(inner);
            case CALL_STMT -> toCallStmt(inner);
            case IF_STMT -> toIfStmt(inner);
            case WHILE_STMT -> toWhileStmt(inner);
            case FOR_STMT -> toForStmt(inner);
            case REPEAT_UNTIL_STMT -> toRepeatUntilStmt(inner);
            case RETURN_STMT -> toReturnStmt(inner);
            case BREAK_STMT -> new BreakStmtNode(span(inner));
            case CONTINUE_STMT -> new ContinueStmtNode(span(inner));
            case ASSERT_STMT -> toAssertStmt(inner);
            case READ_STMT -> toReadStmt(inner);
            case PRINT_STMT -> toPrintStmt(inner);
            case BLOCK_STMT -> new BlockStmtNode(span(inner), toBlock(child(inner.children(), 0)));
            case ABM_CALL_STMT -> toAbmCallStmt(inner);
            case SPAWN_STMT -> toSpawnStmt(inner);
            case MOVE_STMT -> toMoveStmt(inner);
            case STEP_STMT -> toStepStmt(inner);
            case DESTROY_STMT -> toDestroyStmt(inner);
            default -> new PlaceholderStmtNode(span(inner));
        };
    }

    private static AssignStmtNode toAssignStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        ExprNode lvalue = toExprOrPlaceholder(child(c, 0));
        ExprNode value = toExpr(child(c, 1));
        return new AssignStmtNode(span(n), lvalue, value != null ? value : new PlaceholderExprNode(span(n)));
    }

    private static CallStmtNode toCallStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String name = identLexeme(c, 0);
        List<ExprNode> args = toExprList(child(c, 1));
        return new CallStmtNode(span(n), name, args);
    }

    private static List<ExprNode> toExprList(ParseTreeNode n) {
        if (n == null) return List.of();
        return n.children().stream().map(ParseTreeToAst::toExpr).filter(x -> x != null).collect(Collectors.toList());
    }

    private static ExprNode toExprOrPlaceholder(ParseTreeNode n) {
        if (n == null) return new PlaceholderExprNode(SourceSpan.of(0, 0));
        if (n.isTerminal()) return new PlaceholderExprNode(span(n));
        return toExpr(n);
    }

    private static IfStmtNode toIfStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        ExprNode condition = toExpr(child(c, 0));
        StatementNode thenBranch = toStatement(child(c, 1));
        StatementNode elseBranch = c.size() > 2 ? toStatement(child(c, 2)) : null;
        return new IfStmtNode(span(n), condition, thenBranch, elseBranch);
    }

    private static WhileStmtNode toWhileStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        return new WhileStmtNode(span(n), toExpr(child(c, 0)), toStatement(child(c, 1)));
    }

    private static ForStmtNode toForStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        ForInitNode init = toForInit(child(c, 0));
        ExprNode condition = toExpr(child(c, 1));
        List<AssignStmtNode> update = toAssignStmtList(child(c, 2));
        StatementNode body = toStatement(child(c, 3));
        return new ForStmtNode(span(n), init, condition, update, body);
    }

    private static ForInitNode toForInit(ParseTreeNode n) {
        if (n == null) return null;
        if (n.kind() == ParseTreeKind.FOR_INIT_VAR_DECL) {
            List<ParseTreeNode> c = n.children();
            return new ForInitVarDecl(toDataType(child(c, 0)), toDeclaratorList(child(c, 1)));
        }
        if (n.kind() == ParseTreeKind.FOR_INIT_ASSIGN_LIST) {
            List<AssignStmtNode> list = toAssignStmtList(n);
            return new ForInitAssignList(list);
        }
        return null;
    }

    private static List<AssignStmtNode> toAssignStmtList(ParseTreeNode n) {
        if (n == null || n.kind() != ParseTreeKind.ASSIGN_STMT_LIST) return List.of();
        return n.children().stream().map(ParseTreeToAst::toAssignStmt).collect(Collectors.toList());
    }

    private static RepeatUntilStmtNode toRepeatUntilStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        return new RepeatUntilStmtNode(span(n), toBlock(child(c, 0)), toExpr(child(c, 1)));
    }

    private static ReturnStmtNode toReturnStmt(ParseTreeNode n) {
        ExprNode value = child(n.children(), 0) != null ? toExpr(child(n.children(), 0)) : null;
        return new ReturnStmtNode(span(n), value);
    }

    private static AssertStmtNode toAssertStmt(ParseTreeNode n) {
        return new AssertStmtNode(span(n), toExpr(child(n.children(), 0)));
    }

    private static ReadStmtNode toReadStmt(ParseTreeNode n) {
        ExprNode lvalue = toExprOrPlaceholder(child(n.children(), 0));
        return new ReadStmtNode(span(n), lvalue);
    }

    private static PrintStmtNode toPrintStmt(ParseTreeNode n) {
        List<ExprNode> args = toExprList(child(n.children(), 0));
        return new PrintStmtNode(span(n), args);
    }

    private static AbmCallStmtNode toAbmCallStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String name = c.isEmpty() ? "" : (c.get(0).isTerminal() ? c.get(0).token().lexeme() : "");
        List<ExprNode> args = c.size() > 1 ? toExprList(c.get(1)) : List.of();
        return new AbmCallStmtNode(span(n), name, args);
    }

    private static SpawnStmtNode toSpawnStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String agentType = identLexeme(c, 0);
        List<ExprNode> args = toExprList(child(c, 1));
        return new SpawnStmtNode(span(n), agentType, args);
    }

    private static MoveStmtNode toMoveStmt(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        ExprNode z = c.size() > 3 ? toExpr(child(c, 3)) : null;
        return new MoveStmtNode(span(n), toExpr(child(c, 0)), toExpr(child(c, 1)), z);
    }

    private static StepStmtNode toStepStmt(ParseTreeNode n) {
        ExprNode arg = child(n.children(), 0) != null ? toExpr(child(n.children(), 0)) : null;
        return new StepStmtNode(span(n), arg);
    }

    private static DestroyStmtNode toDestroyStmt(ParseTreeNode n) {
        return new DestroyStmtNode(span(n), toExpr(child(n.children(), 0)));
    }

    private static ExprNode toExpr(ParseTreeNode n) {
        if (n == null) return null;
        if (n.kind() == ParseTreeKind.EXPR || n.kind() == ParseTreeKind.COND_EXPR || n.kind() == ParseTreeKind.OR_EXPR
                || n.kind() == ParseTreeKind.AND_EXPR || n.kind() == ParseTreeKind.EQ_EXPR || n.kind() == ParseTreeKind.REL_EXPR
                || n.kind() == ParseTreeKind.ADD_EXPR || n.kind() == ParseTreeKind.MUL_EXPR) {
            List<ParseTreeNode> c = n.children();
            if (c.size() == 1) return toExpr(c.get(0));
            if (c.size() == 3 && n.kind() == ParseTreeKind.COND_EXPR) {
                return new TernaryExprNode(span(n), toExpr(c.get(0)), toExpr(c.get(1)), toExpr(c.get(2)));
            }
            if (c.size() >= 2) {
                String op = c.get(1).isTerminal() ? c.get(1).token().lexeme() : "";
                ExprNode left = toExpr(c.get(0));
                ExprNode right = toExpr(c.get(2));
                if (left != null && right != null) return new BinaryExprNode(span(n), left, op, right);
            }
            return toExpr(c.get(0));
        }
        if (n.kind() == ParseTreeKind.UNARY_EXPR) {
            List<ParseTreeNode> c = n.children();
            if (c.size() >= 2) {
                String op = c.get(0).isTerminal() ? c.get(0).token().lexeme() : "";
                return new UnaryExprNode(span(n), op, toExpr(c.get(1)));
            }
            return toExpr(child(c, 0));
        }
        if (n.kind() == ParseTreeKind.PRIMARY) {
            return toPrimary(n);
        }
        if (n.kind() == ParseTreeKind.ATOM) return toAtom(n);
        if (n.kind() == ParseTreeKind.ABM_CALL_EXPR) return toAbmCallExpr(n);
        if (n.kind() == ParseTreeKind.CONSTANT) return toConstant(n);
        return null;
    }

    private static ExprNode toPrimary(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        if (c.isEmpty()) return null;
        ExprNode e = toExpr(c.get(0));
        for (int i = 1; i < c.size(); i++) {
            ParseTreeNode op = c.get(i);
            if (op.isTerminal()) {
                if (op.token().type() == TokenType.LPAREN) {
                    List<ExprNode> args = i + 1 < c.size() ? toExprList(c.get(i + 1)) : List.of();
                    String name = e instanceof IdentExprNode ident ? ident.name() : "";
                    e = new CallExprNode(span(n), name, args);
                    i++;
                } else if (op.token().type() == TokenType.LBRACKET) {
                    ExprNode index = i + 1 < c.size() ? toExpr(c.get(i + 1)) : null;
                    e = new BinaryExprNode(span(n), e, "[]", index != null ? index : new PlaceholderExprNode(span(n)));
                    i++;
                } else if (op.token().type() == TokenType.DOT) {
                    String field = i + 1 < c.size() ? identLexeme(c.get(i + 1).children(), 0) : "";
                    e = new BinaryExprNode(span(n), e, ".", new IdentExprNode(span(n), field));
                    i++;
                }
            }
        }
        return e;
    }

    private static ExprNode toAtom(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        if (c.isEmpty()) return null;
        ParseTreeNode first = c.get(0);
        if (first.isTerminal()) {
            Token t = first.token();
            if (t.type() == TokenType.LPAREN && c.size() > 1) return new ParenExprNode(span(n), toExpr(c.get(1)));
            if (t.type() == TokenType.INT_LIT || t.type() == TokenType.FLOAT_LIT || t.type() == TokenType.CHAR_LIT
                    || t.type() == TokenType.STRING_LIT || t.type() == TokenType.TRUE || t.type() == TokenType.FALSE) {
                return new LiteralExprNode(span(n), t.literal() != null ? t.literal() : t.lexeme());
            }
            if (t.type() == TokenType.NULL) return new NullExprNode(span(n));
            if (t.type() == TokenType.IDENT) return new IdentExprNode(span(n), t.lexeme());
            if (t.type() == TokenType.SELF) {
                if (c.size() > 2) return new SelfFieldExprNode(span(n), c.get(2).token().lexeme());
                return new SelfExprNode(span(n));
            }
        }
        if (first.kind() == ParseTreeKind.ABM_CALL_EXPR) return toAbmCallExpr(first);
        if (first.kind() == ParseTreeKind.EXPR || first.kind() == ParseTreeKind.COND_EXPR) return new ParenExprNode(span(n), toExpr(first));
        return toExpr(first);
    }

    private static ExprNode toAbmCallExpr(ParseTreeNode n) {
        List<ParseTreeNode> c = n.children();
        String name = c.isEmpty() ? "" : (c.get(0).isTerminal() ? c.get(0).token().lexeme() : "");
        List<ExprNode> args = c.size() > 1 ? toExprList(c.get(1)) : List.of();
        return new AbmCallExprNode(span(n), name, args);
    }

    private static ExprNode toConstant(ParseTreeNode n) {
        ParseTreeNode t = child(n.children(), 0);
        if (t != null && t.isTerminal()) {
            Token tok = t.token();
            return new LiteralExprNode(span(n), tok.literal() != null ? tok.literal() : tok.lexeme());
        }
        return new LiteralExprNode(span(n), 0);
    }
}
