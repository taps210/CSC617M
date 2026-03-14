package src;

import static src.Ast.*;
import src.errors.ParseException;
import src.parsetree.ParseTreeNode;
import src.parsetree.ParseTreeKind;
import src.parsetree.ParseTreeToAst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Parser {
    private final List<Token> tokens;
    private final Appendable out;
    private boolean trace = true;
    private int current = 0;
    private int constructCount = 0;

    public Parser(List<Token> tokens, Appendable out) {
        this.tokens = tokens;
        this.out = out;
    }

    public int getConstructCount() {
        return constructCount;
    }

    private void emit(String construct, Token at) {
        constructCount++;
        if (!trace) return;
        try {
            out.append(construct)
               .append(" found in line ").append(Integer.toString(at.line()))
               .append(" column ").append(Integer.toString(at.col()))
               .append(System.lineSeparator());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProgramNode lastAst;
    private ParseTreeNode lastParseTree;

    public void parseProgram() {
        lastAst = null;
        lastParseTree = null;
        ParseTreeNode programNode = parseProgramParseTree();
        consume(TokenType.EOF, "Expected end of file.");
        lastParseTree = programNode;
        lastAst = ParseTreeToAst.convert(programNode);
    }

    /** Build parse tree for program, then convert to AST. */
    public ProgramNode parseProgramToAst() {
        ParseTreeNode programNode = parseProgramParseTree();
        lastParseTree = programNode;
        return ParseTreeToAst.convert(programNode);
    }

    private ParseTreeNode parseProgramParseTree() {
        Token start = tokens.isEmpty() || isAtEnd() ? null : peek();
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(useList());
        children.add(typeDeclList());
        children.add(constDeclList());
        children.add(globalVarDeclList());
        children.add(funcDeclList());
        children.add(mainFunction());
        return ParseTreeNode.of(ParseTreeKind.PROGRAM, children, start);
    }

    public ProgramNode getProgramNode() {
        return lastAst;
    }

    public ParseTreeNode getParseTreeRoot() {
        return lastParseTree;
    }

    private static SourceSpan span(Token t) {
        return t != null ? SourceSpan.of(t.line(), t.col()) : SourceSpan.of(1, 1);
    }

    private static ParseTreeNode terminal(Token t) {
        return ParseTreeNode.terminal(t);
    }

    // -------------------------
    // Helpers
    // -------------------------
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }
    private boolean isAtEnd() { return peek().type() == TokenType.EOF; }

    // Check if the current token is of the given type.
    private boolean check(TokenType t) {
        return peek().type() == t;
    }

    // Consume and return the current token, unless we're at the end, then return EOF.
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // If the current token is one of the given types, consume it and return true. Otherwise return false.
    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    // Consume the current token if it matches the expected type, otherwise throw an error with the given message.
    private Token consume(TokenType t, String message) {
        if (check(t)) return advance();
        Token p = peek();
        throw new ParseException(message + " Found: " + p.type() + " \"" + p.lexeme() + "\"", p.line(), p.col());
    }

    // Used when IDENT can start either a type name or a variable name.
    // In grammar, a "type" can start with a keyword type (int/float/...) OR IDENT.
    private boolean startsDataType() {
        return check(TokenType.INT) || check(TokenType.FLOAT) || check(TokenType.CHAR)
                || check(TokenType.STRING) || check(TokenType.BOOL) || check(TokenType.IDENT);
    }

    // For for-init ambiguity:
    // if it starts with IDENT and next is IDENT or STAR -> treat as type decl (e.g., Person* p)
    // otherwise treat as assignment list (e.g., i = 0)
    private boolean looksLikeVarDeclNoSemi() {
        if (check(TokenType.INT) || check(TokenType.FLOAT) || check(TokenType.CHAR)
                || check(TokenType.STRING) || check(TokenType.BOOL)) return true;

        if (check(TokenType.IDENT)) {
            TokenType next = tokens.get(Math.min(current + 1, tokens.size() - 1)).type();
            return next == TokenType.IDENT || next == TokenType.STAR;
        }
        return false;
    }

    // Similar to above, but for deciding if a statement starts with a var decl (used in block bodies).
    private boolean looksLikeVarDeclStart() {
        // Primitive types always start a declaration
        if (check(TokenType.INT) || check(TokenType.FLOAT) || check(TokenType.CHAR)
                || check(TokenType.STRING) || check(TokenType.BOOL)) return true;
        // User-defined type: IDENT IDENT ...   (e.g., Foo a;)
        // Pointer type: IDENT * ...            (e.g., Foo* p;)
        if (check(TokenType.IDENT)) {
            TokenType next = tokens.get(Math.min(current + 1, tokens.size() - 1)).type();
            return next == TokenType.IDENT || next == TokenType.STAR;
        }
        return false;
    }

    // Heuristic to decide if we are at the start of a function declaration (used in global scope).
    private boolean looksLikeFuncDeclStart() {
        // void IDENT '(' ...
        if (check(TokenType.VOID)) {
            TokenType t1 = tokens.get(Math.min(current + 1, tokens.size() - 1)).type();
            TokenType t2 = tokens.get(Math.min(current + 2, tokens.size() - 1)).type();
            return t1 == TokenType.IDENT && t2 == TokenType.LPAREN;
        }

        // <data_type> IDENT '(' ...
        // This includes:
        //   int f( ... )
        //   Foo f( ... )
        //   Foo* f( ... )
        if (looksLikeVarDeclStart()) {
            int i = current;

            // consume base type
            if (tokens.get(i).type() == TokenType.IDENT ||
                tokens.get(i).type() == TokenType.INT || tokens.get(i).type() == TokenType.FLOAT ||
                tokens.get(i).type() == TokenType.CHAR || tokens.get(i).type() == TokenType.STRING ||
                tokens.get(i).type() == TokenType.BOOL) {

                i++;

                // consume pointer suffix: STAR*
                while (i < tokens.size() && tokens.get(i).type() == TokenType.STAR) i++;

                // now should be function name IDENT then '('
                if (i + 1 < tokens.size()
                        && tokens.get(i).type() == TokenType.IDENT
                        && tokens.get(i + 1).type() == TokenType.LPAREN) {
                    return true;
                }
            }
        }

        return false;
}

    // -------------------------
    // Program Structure
    // -------------------------
    private ParseTreeNode useList() {
        List<ParseTreeNode> list = new ArrayList<>();
        Token at = null;
        while (match(TokenType.USE)) {
            at = previous();
            emit("USE statement", at);
            ParseTreeNode filenameNode = filename();
            list.add(ParseTreeNode.of(ParseTreeKind.USE_STMT, List.of(filenameNode), at));
            consume(TokenType.SEMI, "Expected ';' after use filename.");
        }
        return ParseTreeNode.of(ParseTreeKind.USE_LIST, list, at);
    }

    private ParseTreeNode filename() {
        List<ParseTreeNode> children = new ArrayList<>();
        Token t = consume(TokenType.IDENT, "Expected identifier in filename.");
        children.add(terminal(t));
        while (match(TokenType.DOT)) {
            children.add(terminal(previous()));
            Token t2 = consume(TokenType.IDENT, "Expected identifier after '.' in filename.");
            children.add(terminal(t2));
        }
        return ParseTreeNode.of(ParseTreeKind.FILENAME, children, null);
    }

    // -------------------------
    // Type Declarations
    // -------------------------
    private ParseTreeNode typeDeclList() {
        List<ParseTreeNode> list = new ArrayList<>();
        while (check(TokenType.TYPE) || check(TokenType.AGENT) || check(TokenType.WORLD)) {
            list.add(typeDecl());
        }
        return ParseTreeNode.of(ParseTreeKind.TYPE_DECL_LIST, list, null);
    }

    private ParseTreeNode typeDecl() {
        if (match(TokenType.TYPE)) {
            Token at = previous();
            emit("Type alias declaration", at);
            Token nameTok = consume(TokenType.IDENT, "Expected type name after 'type'.");
            consume(TokenType.ASSIGN, "Expected '=' after type name.");
            ParseTreeNode rt = recordType();
            consume(TokenType.SEMI, "Expected ';' after type alias.");
            return ParseTreeNode.of(ParseTreeKind.TYPE_ALIAS_DECL, List.of(terminal(nameTok), rt), at);
        }
        if (check(TokenType.AGENT)) return agentDecl();
        if (check(TokenType.WORLD)) return worldDecl();
        Token p = peek();
        throw new ParseException("Expected type declaration.", p.line(), p.col());
    }

    private ParseTreeNode recordType() {
        Token rec = consume(TokenType.RECORD, "Expected 'record'.");
        consume(TokenType.LBRACE, "Expected '{' after record.");
        ParseTreeNode fields = fieldDeclList();
        consume(TokenType.RBRACE, "Expected '}' after record fields.");
        return ParseTreeNode.of(ParseTreeKind.RECORD_TYPE, List.of(fields), rec);
    }

    private ParseTreeNode agentDecl() {
        Token t = consume(TokenType.AGENT, "Expected 'agent'.");
        emit("AGENT declaration", t);
        Token nameTok = consume(TokenType.IDENT, "Expected agent name.");
        consume(TokenType.LBRACE, "Expected '{' after agent name.");
        ParseTreeNode fields = fieldDeclList();
        ParseTreeNode zones = zoneDeclList();
        ParseTreeNode updateBlock = updateBlock();
        consume(TokenType.RBRACE, "Expected '}' after agent body.");
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(terminal(nameTok));
        children.add(fields);
        children.add(zones);
        children.add(updateBlock);
        return ParseTreeNode.of(ParseTreeKind.AGENT_DECL, children, t);
    }

    private ParseTreeNode zoneDeclList() {
        List<ParseTreeNode> list = new ArrayList<>();
        while (check(TokenType.ZONE)) list.add(zoneDecl());
        return ParseTreeNode.of(ParseTreeKind.ZONE_DECL_LIST, list, null);
    }

    private ParseTreeNode zoneDecl() {
        Token t = consume(TokenType.ZONE, "Expected 'zone'.");
        emit("ZONE declaration", t);
        Token nameTok = consume(TokenType.IDENT, "Expected zone name.");
        consume(TokenType.LPAREN, "Expected '(' after zone name.");
        ParseTreeNode condition = expr();
        consume(TokenType.COMMA, "Expected ',' in zone parameters.");
        Token targetTok = consume(TokenType.IDENT, "Expected target type identifier in zone.");
        consume(TokenType.RPAREN, "Expected ')' after zone parameters.");
        ParseTreeNode blk = block();
        List<ParseTreeNode> children = List.of(terminal(nameTok), condition, terminal(targetTok), blk);
        return ParseTreeNode.of(ParseTreeKind.ZONE_DECL, children, t);
    }

    private ParseTreeNode updateBlock() {
        Token t = consume(TokenType.UPDATE, "Expected 'update' block.");
        emit("UPDATE block", t);
        return ParseTreeNode.of(ParseTreeKind.UPDATE_BLOCK, List.of(block()), t);
    }

    private ParseTreeNode worldDecl() {
        Token t = consume(TokenType.WORLD, "Expected 'world'.");
        emit("WORLD declaration", t);
        Token nameTok = consume(TokenType.IDENT, "Expected world name.");
        consume(TokenType.LBRACE, "Expected '{' after world name.");
        ParseTreeNode fields = fieldDeclList();
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(terminal(nameTok));
        children.add(fields);
        if (match(TokenType.PRE)) {
            emit("PRE block", previous());
            children.add(block());
        }
        if (match(TokenType.POST)) {
            emit("POST block", previous());
            children.add(block());
        }
        consume(TokenType.RBRACE, "Expected '}' after world body.");
        return ParseTreeNode.of(ParseTreeKind.WORLD_DECL, children, t);
    }

    // -------------------------
    // Fields / Vars / Consts
    // -------------------------
    private ParseTreeNode fieldDeclList() {
        List<ParseTreeNode> list = new ArrayList<>();
        // Use same heuristic as globalVarDeclList: only parse var decl when it looks like
        // "type name" or "type * name" (not "ident =" which is an assignment).
        while (looksLikeVarDeclStart()) {
            list.add(varDecl());
            consume(TokenType.SEMI, "Expected ';' after field declaration.");
        }
        return ParseTreeNode.of(ParseTreeKind.FIELD_DECL_LIST, list, null);
    }

    private ParseTreeNode constDeclList() {
        List<ParseTreeNode> list = new ArrayList<>();
        while (match(TokenType.CONST)) {
            Token nameTok = consume(TokenType.IDENT, "Expected constant name.");
            consume(TokenType.ASSIGN, "Expected '=' in const declaration.");
            ParseTreeNode value = constant();
            consume(TokenType.SEMI, "Expected ';' after const declaration.");
            list.add(ParseTreeNode.of(ParseTreeKind.CONST_DECL, List.of(terminal(nameTok), value), nameTok));
        }
        return ParseTreeNode.of(ParseTreeKind.CONST_DECL_LIST, list, null);
    }

    private ParseTreeNode constant() {
        if (match(TokenType.INT_LIT, TokenType.FLOAT_LIT, TokenType.CHAR_LIT, TokenType.STRING_LIT,
                TokenType.TRUE, TokenType.FALSE)) {
            return ParseTreeNode.of(ParseTreeKind.CONSTANT, List.of(terminal(previous())), previous());
        }
        Token p = peek();
        throw new ParseException("Expected constant literal.", p.line(), p.col());
    }

    private ParseTreeNode globalVarDeclList() {
        List<ParseTreeNode> list = new ArrayList<>();
        while (looksLikeVarDeclStart() && !looksLikeFuncDeclStart()) {
            list.add(varDecl());
            consume(TokenType.SEMI, "Expected ';' after variable declaration.");
        }
        return ParseTreeNode.of(ParseTreeKind.GLOBAL_VAR_DECL_LIST, list, null);
    }

    private ParseTreeNode varDecl() {
        Token start = peek();
        ParseTreeNode dt = dataType();
        ParseTreeNode decls = declaratorList();
        emit("Variable declaration", start);
        return ParseTreeNode.of(ParseTreeKind.VAR_DECL, List.of(dt, decls), start);
    }

    private ParseTreeNode declaratorList() {
        List<ParseTreeNode> list = new ArrayList<>();
        list.add(declarator());
        while (match(TokenType.COMMA)) list.add(declarator());
        return ParseTreeNode.of(ParseTreeKind.DECLARATOR_LIST, list, null);
    }

    private ParseTreeNode declarator() {
        Token nameTok = consume(TokenType.IDENT, "Expected variable/field name.");
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(terminal(nameTok));
        while (match(TokenType.LBRACKET)) {
            if (match(TokenType.INT_LIT)) {
                children.add(terminal(previous()));
            } else {
                children.add(terminal(peek()));
            }
            consume(TokenType.RBRACKET, "Expected ']' after array dimension.");
        }
        if (match(TokenType.ASSIGN)) {
            children.add(terminal(previous()));
            children.add(expr());
        }
        return ParseTreeNode.of(ParseTreeKind.DECLARATOR, children, nameTok);
    }

    private ParseTreeNode dataType() {
        Token start = peek();
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(baseTypeNode());
        while (match(TokenType.STAR)) children.add(terminal(previous()));
        return ParseTreeNode.of(ParseTreeKind.DATA_TYPE, children, start);
    }

    private ParseTreeNode baseTypeNode() {
        if (match(TokenType.INT)) return terminal(previous());
        if (match(TokenType.FLOAT)) return terminal(previous());
        if (match(TokenType.CHAR)) return terminal(previous());
        if (match(TokenType.STRING)) return terminal(previous());
        if (match(TokenType.BOOL)) return terminal(previous());
        if (check(TokenType.IDENT)) return terminal(advance());
        Token p = peek();
        throw new ParseException("Expected base type.", p.line(), p.col());
    }

    // -------------------------
    // Functions + Main
    // -------------------------
    private ParseTreeNode funcDeclList() {
        List<ParseTreeNode> list = new ArrayList<>();
        while (check(TokenType.VOID) || startsDataType()) {
            if (check(TokenType.VOID) && lookaheadIsMainFunction()) break;
            list.add(funcDecl());
        }
        return ParseTreeNode.of(ParseTreeKind.FUNC_DECL_LIST, list, null);
    }

    private boolean lookaheadIsMainFunction() {
        if (!check(TokenType.VOID)) return false;
        TokenType t1 = tokens.get(Math.min(current + 1, tokens.size() - 1)).type();
        return t1 == TokenType.MAIN;
    }

    private ParseTreeNode funcDecl() {
        ParseTreeNode returnType = returnType();
        Token nameTok = consume(TokenType.IDENT, "Expected function name.");
        emit("FUNCTION declaration", nameTok);
        consume(TokenType.LPAREN, "Expected '(' after function name.");
        ParseTreeNode params = check(TokenType.RPAREN) ? ParseTreeNode.of(ParseTreeKind.PARAM_LIST, List.of(), null) : paramList();
        consume(TokenType.RPAREN, "Expected ')' after parameters.");
        ParseTreeNode body = block();
        return ParseTreeNode.of(ParseTreeKind.FUNC_DECL, List.of(returnType, terminal(nameTok), params, body), nameTok);
    }

    private ParseTreeNode returnType() {
        if (match(TokenType.VOID)) return ParseTreeNode.of(ParseTreeKind.RETURN_TYPE, List.of(terminal(previous())), previous());
        return ParseTreeNode.of(ParseTreeKind.RETURN_TYPE, List.of(dataType()), null);
    }

    private ParseTreeNode paramList() {
        List<ParseTreeNode> list = new ArrayList<>();
        list.add(param());
        while (match(TokenType.COMMA)) list.add(param());
        return ParseTreeNode.of(ParseTreeKind.PARAM_LIST, list, null);
    }

    private ParseTreeNode param() {
        Token start = peek();
        ParseTreeNode dt = dataType();
        Token nameTok = consume(TokenType.IDENT, "Expected parameter name.");
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(dt);
        children.add(terminal(nameTok));
        while (match(TokenType.LBRACKET)) {
            Token lit = consume(TokenType.INT_LIT, "Expected integer size in array dimension.");
            children.add(terminal(lit));
            consume(TokenType.RBRACKET, "Expected ']' after array dimension.");
        }
        return ParseTreeNode.of(ParseTreeKind.PARAM, children, start);
    }

    private ParseTreeNode mainFunction() {
        consume(TokenType.VOID, "Expected 'void' for main.");
        Token mainTok = consume(TokenType.MAIN, "Expected 'main'.");
        emit("MAIN function", mainTok);
        consume(TokenType.LPAREN, "Expected '(' after main.");
        consume(TokenType.RPAREN, "Expected ')' after main.");
        return ParseTreeNode.of(ParseTreeKind.MAIN_FUNCTION, List.of(block()), mainTok);
    }

    // -------------------------
    // Blocks / Statements
    // -------------------------
    private ParseTreeNode block() {
        Token lbrace = consume(TokenType.LBRACE, "Expected '{'.");
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(fieldDeclList());
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            children.add(statement());
        }
        consume(TokenType.RBRACE, "Expected '}' to close block.");
        return ParseTreeNode.of(ParseTreeKind.BLOCK, children, lbrace);
    }

    private ParseTreeNode statement() {
        ParseTreeNode inner;
        if (check(TokenType.SELF)) {
            inner = assignStmt();
            consume(TokenType.SEMI, "Expected ';' after assignment.");
        } else if (check(TokenType.STAR) || check(TokenType.IDENT)) {
            if (check(TokenType.IDENT) && tokens.get(Math.min(current + 1, tokens.size() - 1)).type() == TokenType.LPAREN) {
                inner = callStmt();
                consume(TokenType.SEMI, "Expected ';' after call.");
            } else {
                inner = assignStmt();
                consume(TokenType.SEMI, "Expected ';' after assignment.");
            }
        } else if (check(TokenType.NEIGHBORS) || check(TokenType.RAND)) {
            inner = abmCallStmtNode();
            consume(TokenType.SEMI, "Expected ';' after ABM call.");
        } else if (match(TokenType.READ)) {
            Token kw = previous();
            consume(TokenType.LPAREN, "Expected '(' after read.");
            Token start = peek();
            lvalue();
            consume(TokenType.RPAREN, "Expected ')' after read(...).");
            consume(TokenType.SEMI, "Expected ';' after I/O statement.");
            emit("I/O statement (read)", kw);
            inner = ParseTreeNode.of(ParseTreeKind.READ_STMT, List.of(terminal(start)), kw);
        } else if (match(TokenType.PRINT)) {
            Token kw = previous();
            consume(TokenType.LPAREN, "Expected '(' after print.");
            List<ParseTreeNode> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) { args.add(expr()); while (match(TokenType.COMMA)) args.add(expr()); }
            consume(TokenType.RPAREN, "Expected ')' after print(...).");
            consume(TokenType.SEMI, "Expected ';' after I/O statement.");
            emit("I/O statement (print)", kw);
            inner = ParseTreeNode.of(ParseTreeKind.PRINT_STMT, List.of(ParseTreeNode.of(ParseTreeKind.EXPR_LIST, args, null)), kw);
        } else if (check(TokenType.IF)) { inner = ifStmt(); }
        else if (check(TokenType.WHILE)) { inner = whileStmt(); }
        else if (check(TokenType.FOR)) { inner = forStmt(); }
        else if (check(TokenType.REPEAT)) { inner = repeatUntilStmt(); }
        else if (match(TokenType.RETURN)) {
            Token t = previous();
            ParseTreeNode val = check(TokenType.SEMI) ? null : expr();
            consume(TokenType.SEMI, "Expected ';' after return.");
            emit("RETURN statement", t);
            inner = ParseTreeNode.of(ParseTreeKind.RETURN_STMT, val != null ? List.of(val) : List.of(), t);
        } else if (match(TokenType.BREAK)) {
            Token t = previous();
            consume(TokenType.SEMI, "Expected ';' after break.");
            emit("BREAK statement", t);
            inner = ParseTreeNode.of(ParseTreeKind.BREAK_STMT, List.of(), t);
        } else if (match(TokenType.CONTINUE)) {
            Token t = previous();
            consume(TokenType.SEMI, "Expected ';' after continue.");
            emit("CONTINUE statement", t);
            inner = ParseTreeNode.of(ParseTreeKind.CONTINUE_STMT, List.of(), t);
        } else if (match(TokenType.ASSERT)) {
            Token t = previous();
            consume(TokenType.LPAREN, "Expected '(' after assert.");
            ParseTreeNode cond = expr();
            consume(TokenType.RPAREN, "Expected ')' after assert condition.");
            consume(TokenType.SEMI, "Expected ';' after assert.");
            emit("ASSERT statement", t);
            inner = ParseTreeNode.of(ParseTreeKind.ASSERT_STMT, List.of(cond), t);
        } else if (check(TokenType.LBRACE)) {
            inner = ParseTreeNode.of(ParseTreeKind.BLOCK_STMT, List.of(block()), peek());
        } else if (check(TokenType.SPAWN) || check(TokenType.MOVE) || check(TokenType.STEP) || check(TokenType.DESTROY)) {
            inner = abmStmt();
            consume(TokenType.SEMI, "Expected ';' after ABM statement.");
        } else {
            Token p = peek();
            throw new ParseException("Unexpected statement start: " + p.type(), p.line(), p.col());
        }
        return ParseTreeNode.of(ParseTreeKind.STATEMENT, List.of(inner), inner.token());
    }

    private ParseTreeNode assignStmt() {
        Token start = peek();
        lvalue();
        consume(TokenType.ASSIGN, "Expected '=' in assignment.");
        ParseTreeNode value = expr();
        emit("Assignment statement", start);
        return ParseTreeNode.of(ParseTreeKind.ASSIGN_STMT, List.of(terminal(start), value), start);
    }

    private ParseTreeNode callStmt() {
        Token fn = consume(TokenType.IDENT, "Expected function name.");
        consume(TokenType.LPAREN, "Expected '(' after function name.");
        List<ParseTreeNode> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) { args.add(expr()); while (match(TokenType.COMMA)) args.add(expr()); }
        consume(TokenType.RPAREN, "Expected ')' after arguments.");
        emit("Call statement", fn);
        return ParseTreeNode.of(ParseTreeKind.CALL_STMT, List.of(terminal(fn), ParseTreeNode.of(ParseTreeKind.EXPR_LIST, args, null)), fn);
    }

    private ParseTreeNode ifStmt() {
        Token ifTok = consume(TokenType.IF, "Expected 'if'.");
        emit("IF statement", ifTok);
        consume(TokenType.LPAREN, "Expected '(' after if.");
        ParseTreeNode condition = expr();
        consume(TokenType.RPAREN, "Expected ')' after if condition.");
        ParseTreeNode thenBranch = statement();
        ParseTreeNode elseBranch = null;
        if (match(TokenType.ELSE)) { emit("ELSE clause", previous()); elseBranch = statement(); }
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(condition); children.add(thenBranch);
        if (elseBranch != null) children.add(elseBranch);
        return ParseTreeNode.of(ParseTreeKind.IF_STMT, children, ifTok);
    }

    private ParseTreeNode whileStmt() {
        Token w = consume(TokenType.WHILE, "Expected 'while'.");
        emit("WHILE loop", w);
        consume(TokenType.LPAREN, "Expected '(' after while.");
        ParseTreeNode condition = expr();
        consume(TokenType.RPAREN, "Expected ')' after while condition.");
        return ParseTreeNode.of(ParseTreeKind.WHILE_STMT, List.of(condition, statement()), w);
    }

    private ParseTreeNode forStmt() {
        Token f = consume(TokenType.FOR, "Expected 'for'.");
        emit("FOR loop", f);
        consume(TokenType.LPAREN, "Expected '(' after for.");
        ParseTreeNode init = null;
        if (!check(TokenType.SEMI)) {
            init = looksLikeVarDeclNoSemi() ? forInitVarDecl() : ParseTreeNode.of(ParseTreeKind.FOR_INIT_ASSIGN_LIST, List.of(assignStmtList()), null);
        }
        consume(TokenType.SEMI, "Expected ';' after for-init.");
        ParseTreeNode condition = check(TokenType.SEMI) ? null : expr();
        consume(TokenType.SEMI, "Expected ';' after for-condition.");
        ParseTreeNode update = check(TokenType.RPAREN) ? ParseTreeNode.of(ParseTreeKind.ASSIGN_STMT_LIST, List.of(), null) : assignStmtList();
        consume(TokenType.RPAREN, "Expected ')' after for-update.");
        ParseTreeNode body = statement();
        List<ParseTreeNode> children = new ArrayList<>();
        if (init != null) children.add(init);
        children.add(condition != null ? condition : ParseTreeNode.of(ParseTreeKind.EXPR, List.of(), null));
        children.add(update);
        children.add(body);
        return ParseTreeNode.of(ParseTreeKind.FOR_STMT, children, f);
    }

    private ParseTreeNode forInitVarDecl() {
        ParseTreeNode dt = dataType();
        ParseTreeNode decls = declaratorList();
        return ParseTreeNode.of(ParseTreeKind.FOR_INIT_VAR_DECL, List.of(dt, decls), null);
    }

    private ParseTreeNode assignStmtList() {
        List<ParseTreeNode> list = new ArrayList<>();
        list.add(assignStmt());
        while (match(TokenType.COMMA)) list.add(assignStmt());
        return ParseTreeNode.of(ParseTreeKind.ASSIGN_STMT_LIST, list, null);
    }

    private ParseTreeNode repeatUntilStmt() {
        Token r = consume(TokenType.REPEAT, "Expected 'repeat'.");
        emit("REPEAT loop", r);
        ParseTreeNode blk = block();
        consume(TokenType.UNTIL, "Expected 'until' after repeat block.");
        consume(TokenType.LPAREN, "Expected '(' after until.");
        ParseTreeNode condition = expr();
        consume(TokenType.RPAREN, "Expected ')' after until condition.");
        consume(TokenType.SEMI, "Expected ';' after repeat-until.");
        return ParseTreeNode.of(ParseTreeKind.REPEAT_UNTIL_STMT, List.of(blk, condition), r);
    }

    // -------------------------
    // Lvalues (supports *lvalue)
    // -------------------------
    private void lvalue() {
        if (match(TokenType.STAR)) {
            lvalue();
            return;
        }
        if (match(TokenType.SELF)) {
            consume(TokenType.DOT, "Expected '.' after self.");
            consume(TokenType.IDENT, "Expected field name after '.'.");
            lvalueTail();
            return;
        }
        consume(TokenType.IDENT, "Expected identifier in lvalue.");
        lvalueTail();
    }

    private void lvalueTail() {
        while (true) {
            if (match(TokenType.DOT)) {
                consume(TokenType.IDENT, "Expected field name after '.'.");
                continue;
            }
            if (match(TokenType.LBRACKET)) {
                expr();
                consume(TokenType.RBRACKET, "Expected ']' after index.");
                continue;
            }
            break;
        }
    }

    // -------------------------
    // ABM statements/calls
    // -------------------------
    private ParseTreeNode abmCallStmtNode() {
        Token nameTok = match(TokenType.NEIGHBORS) ? previous() : (match(TokenType.RAND) ? previous() : null);
        if (nameTok == null) throw new ParseException("Expected ABM call", peek().line(), peek().col());
        consume(TokenType.LPAREN, "Expected '(' after ABM call.");
        List<ParseTreeNode> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) { args.add(expr()); while (match(TokenType.COMMA)) args.add(expr()); }
        consume(TokenType.RPAREN, "Expected ')' after ABM call args.");
        return ParseTreeNode.of(ParseTreeKind.ABM_CALL_STMT, List.of(terminal(nameTok), ParseTreeNode.of(ParseTreeKind.EXPR_LIST, args, null)), nameTok);
    }

    private ParseTreeNode abmStmt() {
        if (match(TokenType.SPAWN)) {
            Token t = previous();
            Token typeTok = consume(TokenType.IDENT, "Expected agent type after spawn.");
            consume(TokenType.LPAREN, "Expected '(' after spawn type.");
            List<ParseTreeNode> args = new ArrayList<>();
            if (!check(TokenType.RPAREN)) { args.add(expr()); while (match(TokenType.COMMA)) args.add(expr()); }
            consume(TokenType.RPAREN, "Expected ')' after spawn args.");
            emit("ABM SPAWN statement", t);
            return ParseTreeNode.of(ParseTreeKind.SPAWN_STMT, List.of(terminal(typeTok), ParseTreeNode.of(ParseTreeKind.EXPR_LIST, args, null)), t);
        }
        if (match(TokenType.MOVE)) {
            Token t = previous();
            consume(TokenType.LPAREN, "Expected '(' after move.");
            ParseTreeNode x = expr();
            consume(TokenType.COMMA, "Expected ',' in move.");
            ParseTreeNode y = expr();
            ParseTreeNode z = match(TokenType.COMMA) ? expr() : null;
            consume(TokenType.RPAREN, "Expected ')' after move args.");
            emit("ABM MOVE statement", t);
            List<ParseTreeNode> children = new ArrayList<>();
            children.add(x); children.add(y); if (z != null) children.add(z);
            return ParseTreeNode.of(ParseTreeKind.MOVE_STMT, children, t);
        }
        if (match(TokenType.STEP)) {
            Token t = previous();
            consume(TokenType.LPAREN, "Expected '(' after step.");
            ParseTreeNode arg = check(TokenType.RPAREN) ? null : expr();
            consume(TokenType.RPAREN, "Expected ')' after step args.");
            emit("ABM STEP statement", t);
            return ParseTreeNode.of(ParseTreeKind.STEP_STMT, arg != null ? List.of(arg) : List.of(), t);
        }
        if (match(TokenType.DESTROY)) {
            Token t = previous();
            consume(TokenType.LPAREN, "Expected '(' after destroy.");
            ParseTreeNode target = expr();
            consume(TokenType.RPAREN, "Expected ')' after destroy arg.");
            emit("ABM DESTROY statement", t);
            return ParseTreeNode.of(ParseTreeKind.DESTROY_STMT, List.of(target), t);
        }
        Token p = peek();
        throw new ParseException("Expected ABM statement.", p.line(), p.col());
    }

    // -------------------------
    // Expressions
    // -------------------------
    private ParseTreeNode expr() {
        return ParseTreeNode.of(ParseTreeKind.EXPR, List.of(condExpr()), null);
    }

    private ParseTreeNode condExpr() {
        ParseTreeNode condition = orExpr();
        if (match(TokenType.QMARK)) {
            Token at = previous();
            ParseTreeNode thenExpr = expr();
            consume(TokenType.COLON, "Expected ':' in conditional expression.");
            ParseTreeNode elseExpr = condExpr();
            return ParseTreeNode.of(ParseTreeKind.COND_EXPR, List.of(condition, thenExpr, elseExpr), at);
        }
        return condition;
    }

    private ParseTreeNode orExpr() {
        ParseTreeNode left = andExpr();
        while (match(TokenType.OROR)) {
            Token opTok = previous();
            ParseTreeNode right = andExpr();
            left = ParseTreeNode.of(ParseTreeKind.OR_EXPR, List.of(left, terminal(opTok), right), opTok);
        }
        return left;
    }

    private ParseTreeNode andExpr() {
        ParseTreeNode left = eqExpr();
        while (match(TokenType.ANDAND)) {
            Token opTok = previous();
            ParseTreeNode right = eqExpr();
            left = ParseTreeNode.of(ParseTreeKind.AND_EXPR, List.of(left, terminal(opTok), right), opTok);
        }
        return left;
    }

    private ParseTreeNode eqExpr() {
        ParseTreeNode left = relExpr();
        while (match(TokenType.EQEQ, TokenType.NEQ)) {
            Token opTok = previous();
            ParseTreeNode right = relExpr();
            left = ParseTreeNode.of(ParseTreeKind.EQ_EXPR, List.of(left, terminal(opTok), right), opTok);
        }
        return left;
    }

    private ParseTreeNode relExpr() {
        ParseTreeNode left = addExpr();
        while (match(TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE)) {
            Token opTok = previous();
            ParseTreeNode right = addExpr();
            left = ParseTreeNode.of(ParseTreeKind.REL_EXPR, List.of(left, terminal(opTok), right), opTok);
        }
        return left;
    }

    private ParseTreeNode addExpr() {
        ParseTreeNode left = mulExpr();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token opTok = previous();
            ParseTreeNode right = mulExpr();
            left = ParseTreeNode.of(ParseTreeKind.ADD_EXPR, List.of(left, terminal(opTok), right), opTok);
        }
        return left;
    }

    private ParseTreeNode mulExpr() {
        ParseTreeNode left = unaryExpr();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            Token opTok = previous();
            ParseTreeNode right = unaryExpr();
            left = ParseTreeNode.of(ParseTreeKind.MUL_EXPR, List.of(left, terminal(opTok), right), opTok);
        }
        return left;
    }

    private ParseTreeNode unaryExpr() {
        if (match(TokenType.NOT, TokenType.MINUS, TokenType.PLUS, TokenType.AMP, TokenType.STAR)) {
            Token opTok = previous();
            ParseTreeNode operand = unaryExpr();
            return ParseTreeNode.of(ParseTreeKind.UNARY_EXPR, List.of(terminal(opTok), operand), opTok);
        }
        return primary();
    }

    private ParseTreeNode primary() {
        List<ParseTreeNode> children = new ArrayList<>();
        children.add(atom());
        while (true) {
            if (match(TokenType.LPAREN)) {
                children.add(terminal(previous()));
                List<ParseTreeNode> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) { args.add(expr()); while (match(TokenType.COMMA)) args.add(expr()); }
                children.add(ParseTreeNode.of(ParseTreeKind.EXPR_LIST, args, null));
                consume(TokenType.RPAREN, "Expected ')' after call.");
            } else if (match(TokenType.LBRACKET)) {
                children.add(terminal(previous()));
                children.add(expr());
                consume(TokenType.RBRACKET, "Expected ']' after index.");
            } else if (match(TokenType.DOT)) {
                children.add(terminal(previous()));
                children.add(terminal(consume(TokenType.IDENT, "Expected field name after '.'.")));
            } else break;
        }
        return ParseTreeNode.of(ParseTreeKind.PRIMARY, children, null);
    }

    private ParseTreeNode atom() {
        if (match(TokenType.LPAREN)) {
            Token lparen = previous();
            ParseTreeNode inner = expr();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return ParseTreeNode.of(ParseTreeKind.ATOM, List.of(terminal(lparen), inner), lparen);
        }
        if (match(TokenType.INT_LIT, TokenType.FLOAT_LIT, TokenType.CHAR_LIT, TokenType.STRING_LIT,
                TokenType.TRUE, TokenType.FALSE)) {
            Token t = previous();
            return ParseTreeNode.of(ParseTreeKind.ATOM, List.of(terminal(t)), t);
        }
        if (match(TokenType.NULL)) {
            return ParseTreeNode.of(ParseTreeKind.ATOM, List.of(terminal(previous())), previous());
        }
        if (match(TokenType.IDENT)) {
            return ParseTreeNode.of(ParseTreeKind.ATOM, List.of(terminal(previous())), previous());
        }
        if (check(TokenType.NEIGHBORS) || check(TokenType.RAND)) {
            return abmCallExprParseTree();
        }
        if (match(TokenType.SELF)) {
            Token t = previous();
            if (match(TokenType.DOT)) {
                Token field = consume(TokenType.IDENT, "Expected field name after '.'.");
                return ParseTreeNode.of(ParseTreeKind.ATOM, List.of(terminal(t), terminal(previous()), terminal(field)), t);
            }
            return ParseTreeNode.of(ParseTreeKind.ATOM, List.of(terminal(t)), t);
        }
        Token p = peek();
        throw new ParseException("Expected expression atom.", p.line(), p.col());
    }

    private ParseTreeNode abmCallExprParseTree() {
        Token nameTok = match(TokenType.NEIGHBORS) ? previous() : (match(TokenType.RAND) ? previous() : null);
        if (nameTok == null) throw new ParseException("Expected ABM call", peek().line(), peek().col());
        consume(TokenType.LPAREN, "Expected '(' after ABM call.");
        List<ParseTreeNode> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) { args.add(expr()); while (match(TokenType.COMMA)) args.add(expr()); }
        consume(TokenType.RPAREN, "Expected ')' after ABM call args.");
        return ParseTreeNode.of(ParseTreeKind.ABM_CALL_EXPR, List.of(terminal(nameTok), ParseTreeNode.of(ParseTreeKind.EXPR_LIST, args, null)), nameTok);
    }
}