package src;

import java.io.IOException;
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

    // emit the construct found at the current token.
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

    // Entry point
    public void parseProgram() {
        useList();
        typeDeclList();
        constDeclList();
        globalVarDeclList();
        funcDeclList();
        mainFunction();
        consume(TokenType.EOF, "Expected end of file.");
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
    private void useList() {
        while (match(TokenType.USE)) {
            emit("USE statement", previous());
            filename();
            consume(TokenType.SEMI, "Expected ';' after use filename.");
        }
    }

    private void filename() {
        consume(TokenType.IDENT, "Expected identifier in filename.");
        while (match(TokenType.DOT)) {
            consume(TokenType.IDENT, "Expected identifier after '.' in filename.");
        }
    }

    // -------------------------
    // Type Declarations
    // -------------------------
    private void typeDeclList() {
        while (check(TokenType.TYPE) || check(TokenType.AGENT) || check(TokenType.WORLD)) {
            typeDecl();
        }
    }

    private void typeDecl() {
        if (match(TokenType.TYPE)) {
            emit("Type alias declaration", previous());
            consume(TokenType.IDENT, "Expected type name after 'type'.");
            consume(TokenType.ASSIGN, "Expected '=' after type name.");
            recordType();
            consume(TokenType.SEMI, "Expected ';' after type alias.");
            return;
        }
        if (check(TokenType.AGENT)) { agentDecl(); return; }
        if (check(TokenType.WORLD)) { worldDecl(); return; }

        Token p = peek();
        throw new ParseException("Expected type declaration.", p.line(), p.col());
    }

    private void recordType() {
        consume(TokenType.RECORD, "Expected 'record'.");
        consume(TokenType.LBRACE, "Expected '{' after record.");
        fieldDeclList();
        consume(TokenType.RBRACE, "Expected '}' after record fields.");
    }

    private void agentDecl() {
        Token t = consume(TokenType.AGENT, "Expected 'agent'.");
        emit("AGENT declaration", t);
        consume(TokenType.IDENT, "Expected agent name.");
        consume(TokenType.LBRACE, "Expected '{' after agent name.");
        agentBody();
        consume(TokenType.RBRACE, "Expected '}' after agent body.");
    }

    private void agentBody() {
        fieldDeclList();
        zoneDeclList();
        updateBlock();
    }

    private void worldDecl() {
        Token t = consume(TokenType.WORLD, "Expected 'world'.");
        emit("WORLD declaration", t);
        consume(TokenType.IDENT, "Expected world name.");
        consume(TokenType.LBRACE, "Expected '{' after world name.");
        worldBody();
        consume(TokenType.RBRACE, "Expected '}' after world body.");
    }

    private void worldBody() {
        fieldDeclList();
        if (match(TokenType.PRE)) {
            emit("PRE block", previous());
            block();
        }

        if (match(TokenType.POST)) {
            emit("POST block", previous());
            block();
        }
    }

    private void updateBlock() {
        Token t = consume(TokenType.UPDATE, "Expected 'update' block.");
        emit("UPDATE block", t);
        block();
    }

    private void zoneDeclList() {
        while (check(TokenType.ZONE)) {
            zoneDecl();
        }
    }

    private void zoneDecl() {
        Token t = consume(TokenType.ZONE, "Expected 'zone'.");
        emit("ZONE declaration", t);

        consume(TokenType.IDENT, "Expected zone name.");
        consume(TokenType.LPAREN, "Expected '(' after zone name.");
        expr();
        consume(TokenType.COMMA, "Expected ',' in zone parameters.");
        consume(TokenType.IDENT, "Expected target type identifier in zone.");
        consume(TokenType.RPAREN, "Expected ')' after zone parameters.");
        block();
    }

    // -------------------------
    // Fields / Vars / Consts
    // -------------------------
    private void fieldDeclList() {
        while (startsDataType()) {
            // Heuristic: a field decl is type + declarator_list, followed by ';'
            // but type could also be IDENT in statements; here we are inside record/agent/world bodies
            // where fields are expected before blocks. This is fine.
            fieldDecl();
            consume(TokenType.SEMI, "Expected ';' after field declaration.");
        }
    }

    private void fieldDecl() {
        dataType();
        declaratorList();
    }

    private void constDeclList() {
        while (match(TokenType.CONST)) {
            consume(TokenType.IDENT, "Expected constant name.");
            consume(TokenType.ASSIGN, "Expected '=' in const declaration.");
            constant();
            consume(TokenType.SEMI, "Expected ';' after const declaration.");
        }
    }

    private void constant() {
        if (match(TokenType.INT_LIT, TokenType.FLOAT_LIT, TokenType.CHAR_LIT, TokenType.STRING_LIT,
                TokenType.TRUE, TokenType.FALSE)) return;
        Token p = peek();
        throw new ParseException("Expected constant literal.", p.line(), p.col());
    }

    private void globalVarDeclList() {
        // Only parse global var declarations while it does NOT look like a function declaration.
        while (looksLikeVarDeclStart() && !looksLikeFuncDeclStart()) {
            varDecl();
        }
    }

    private void varDecl() {
        Token start = peek();
        dataType();
        declaratorList();
        consume(TokenType.SEMI, "Expected ';' after variable declaration.");
        emit("Variable declaration", start);
    }

    private void declaratorList() {
        declarator();
        while (match(TokenType.COMMA)) {
            declarator();
        }
    }

    private void declarator() {
        consume(TokenType.IDENT, "Expected variable/field name.");
        // array dims
        while (match(TokenType.LBRACKET)) {
            consume(TokenType.INT_LIT, "Expected integer size in array dimension.");
            consume(TokenType.RBRACKET, "Expected ']' after array dimension.");
        }
        // init opt
        if (match(TokenType.ASSIGN)) {
            expr();
        }
    }

    // -------------------------
    // Data Types with pointers
    // <data_type> -> <base_type> <pointer_suffix>
    // <pointer_suffix> -> * <pointer_suffix> | ε
    // -------------------------
    private void dataType() {
        baseType();
        while (match(TokenType.STAR)) {
            // pointer suffix
        }
    }

    private void baseType() {
        if (match(TokenType.INT, TokenType.FLOAT, TokenType.CHAR, TokenType.STRING, TokenType.BOOL)) return;
        if (match(TokenType.IDENT)) return; // user-defined types
        Token p = peek();
        throw new ParseException("Expected base type.", p.line(), p.col());
    }

    // -------------------------
    // Functions + Main
    // -------------------------
    private void funcDeclList() {
        while (check(TokenType.VOID) || startsDataType()) {
            // main is separate, so avoid consuming "void main"
            if (check(TokenType.VOID) && lookaheadIsMainFunction()) break;
            funcDecl();
        }
    }

    private boolean lookaheadIsMainFunction() {
        if (!check(TokenType.VOID)) return false;
        TokenType t1 = tokens.get(Math.min(current + 1, tokens.size() - 1)).type();
        return t1 == TokenType.MAIN;
    }

    private void funcDecl() {
        returnType();

        Token name = consume(TokenType.IDENT, "Expected function name.");
        emit("FUNCTION declaration", name);

        consume(TokenType.LPAREN, "Expected '(' after function name.");
        if (!check(TokenType.RPAREN)) paramList();
        consume(TokenType.RPAREN, "Expected ')' after parameters.");
        block();
    }

    private void returnType() {
        if (match(TokenType.VOID)) return;
        dataType();
    }

    private void paramList() {
        param();
        while (match(TokenType.COMMA)) {
            param();
        }
    }

    private void param() {
        dataType();
        consume(TokenType.IDENT, "Expected parameter name.");
        // optional array dims
        while (match(TokenType.LBRACKET)) {
            consume(TokenType.INT_LIT, "Expected integer size in array dimension.");
            consume(TokenType.RBRACKET, "Expected ']' after array dimension.");
        }
    }

    private void mainFunction() {
        consume(TokenType.VOID, "Expected 'void' for main.");
        Token mainTok = consume(TokenType.MAIN, "Expected 'main'.");
        emit("MAIN function", mainTok);
        consume(TokenType.LPAREN, "Expected '(' after main.");
        consume(TokenType.RPAREN, "Expected ')' after main.");
        block();
    }

    // -------------------------
    // Blocks / Statements
    // -------------------------
    private void block() {
        consume(TokenType.LBRACE, "Expected '{'.");
        while (looksLikeVarDeclStart()) {
            varDecl();
        }
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            statement();
        }
        consume(TokenType.RBRACE, "Expected '}' to close block.");
    }

    private void statement() {
        // Assignment starting with 'self.' (lvalue: self . IDENT lvalue_tail)
        if (check(TokenType.SELF)) {
            assignStmt();
            consume(TokenType.SEMI, "Expected ';' after assignment.");
            return;
        }
        // Assignment starting with '*' or IDENT (lvalue)
        if (check(TokenType.STAR) || check(TokenType.IDENT)) {
            // Decide assignment vs call_stmt:
            // If it looks like a call: IDENT '('
            if (check(TokenType.IDENT) && tokens.get(Math.min(current + 1, tokens.size() - 1)).type() == TokenType.LPAREN) {
                callStmt();
                consume(TokenType.SEMI, "Expected ';' after call.");
                return;
            }
            // Otherwise attempt assignment
            assignStmt();
            consume(TokenType.SEMI, "Expected ';' after assignment.");
            return;
        }

        // ABM call statements (neighbors(...); rand(...);)
        if (check(TokenType.NEIGHBORS) || check(TokenType.RAND)) {
            abmCall();
            consume(TokenType.SEMI, "Expected ';' after ABM call.");
            return;
        }

        if (match(TokenType.READ, TokenType.PRINT)) {
            // we consumed keyword already, but grammar expects io_stmt; easiest: rewind not worth it.
            // We'll parse based on which we matched:
            Token kw = previous();
            if (kw.type() == TokenType.READ) {
                consume(TokenType.LPAREN, "Expected '(' after read.");
                lvalue();
                consume(TokenType.RPAREN, "Expected ')' after read(...).");
            } else {
                consume(TokenType.LPAREN, "Expected '(' after print.");
                if (!check(TokenType.RPAREN)) {
                    expr();
                    while (match(TokenType.COMMA)) expr();
                }
                consume(TokenType.RPAREN, "Expected ')' after print(...).");
            }
            consume(TokenType.SEMI, "Expected ';' after I/O statement.");
            return;
        }

        if (check(TokenType.IF)) { ifStmt(); return; }
        if (check(TokenType.WHILE)) { whileStmt(); return; }
        if (check(TokenType.FOR)) { forStmt(); return; }
        if (check(TokenType.REPEAT)) { repeatUntilStmt(); return; }

        if (match(TokenType.RETURN)) {
            Token t = previous();
            if (!check(TokenType.SEMI)) expr();
            consume(TokenType.SEMI, "Expected ';' after return.");
            emit("RETURN statement", t);
            return;
        }

        if (match(TokenType.BREAK)) {
            Token t = previous(); 
            consume(TokenType.SEMI, "Expected ';' after break."); 
            emit("BREAK statement", t);
            return; 
        }

        if (match(TokenType.CONTINUE)) { 
            Token t = previous();
            consume(TokenType.SEMI, "Expected ';' after continue."); 
            emit("CONTINUE statement", t);
            return; 
        }

        if (match(TokenType.ASSERT)) {
            Token t = previous();
            consume(TokenType.LPAREN, "Expected '(' after assert.");
            expr();
            consume(TokenType.RPAREN, "Expected ')' after assert condition.");
            consume(TokenType.SEMI, "Expected ';' after assert.");
            emit("ASSERT statement", t);
            return;
        }

        if (check(TokenType.LBRACE)) { block(); return; }

        // ABM statements: spawn/move/step/destroy
        if (check(TokenType.SPAWN) || check(TokenType.MOVE) || check(TokenType.STEP) || check(TokenType.DESTROY)) {
            abmStmt();
            consume(TokenType.SEMI, "Expected ';' after ABM statement.");
            return;
        }

        Token p = peek();
        throw new ParseException("Unexpected statement start: " + p.type(), p.line(), p.col());
    }

    private void assignStmt() {
        Token start = peek();     // assignment begins at first token of lvalue
        lvalue();
        consume(TokenType.ASSIGN, "Expected '=' in assignment.");
        expr();
        emit("Assignment statement", start);
    }

    private void callStmt() {
        // IDENT '(' arg_list_opt ')'
        Token fn = consume(TokenType.IDENT, "Expected function name.");
        consume(TokenType.LPAREN, "Expected '(' after function name.");
        if (!check(TokenType.RPAREN)) {
            expr();
            while (match(TokenType.COMMA)) expr();
        }
        consume(TokenType.RPAREN, "Expected ')' after arguments.");
        emit("Call statement", fn);
    }

    private void ifStmt() {
        Token ifTok = consume(TokenType.IF, "Expected 'if'.");
        emit("IF statement", ifTok);

        consume(TokenType.LPAREN, "Expected '(' after if.");
        expr();
        consume(TokenType.RPAREN, "Expected ')' after if condition.");
        statement();
        if (match(TokenType.ELSE)) {
            emit("ELSE clause", previous());
            statement();
        }
    }

    private void whileStmt() {
        Token w = consume(TokenType.WHILE, "Expected 'while'.");
        emit("WHILE loop", w);

        consume(TokenType.LPAREN, "Expected '(' after while.");
        expr();
        consume(TokenType.RPAREN, "Expected ')' after while condition.");
        statement();
    }

    private void forStmt() {
        Token f = consume(TokenType.FOR, "Expected 'for'.");
        emit("FOR loop", f);

        consume(TokenType.LPAREN, "Expected '(' after for.");

        // init
        if (!check(TokenType.SEMI)) {
            if (looksLikeVarDeclNoSemi()) {
                dataType();
                declaratorList();
            } else {
                assignStmtList();
            }
        }
        consume(TokenType.SEMI, "Expected ';' after for-init.");

        // condition
        if (!check(TokenType.SEMI)) expr();
        consume(TokenType.SEMI, "Expected ';' after for-condition.");

        // update
        if (!check(TokenType.RPAREN)) {
            assignStmtList();
        }
        consume(TokenType.RPAREN, "Expected ')' after for-update.");
        statement();
    }

    private void assignStmtList() {
        assignStmt();
        while (match(TokenType.COMMA)) {
            assignStmt();
        }
    }

    private void repeatUntilStmt() {
        Token r = consume(TokenType.REPEAT, "Expected 'repeat'.");
        emit("REPEAT loop", r);

        block();
        consume(TokenType.UNTIL, "Expected 'until' after repeat block.");
        consume(TokenType.LPAREN, "Expected '(' after until.");
        expr();
        consume(TokenType.RPAREN, "Expected ')' after until condition.");
        consume(TokenType.SEMI, "Expected ';' after repeat-until.");
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
    private void abmStmt() {
        if (match(TokenType.SPAWN)) {
            Token t = previous();
            consume(TokenType.IDENT, "Expected agent type after spawn.");
            consume(TokenType.LPAREN, "Expected '(' after spawn type.");
            if (!check(TokenType.RPAREN)) {
                expr();
                while (match(TokenType.COMMA)) expr();
            }
            consume(TokenType.RPAREN, "Expected ')' after spawn args.");
            emit("ABM SPAWN statement", t);
            return;
        }

        if (match(TokenType.MOVE)) {
            Token t = previous();
            consume(TokenType.LPAREN, "Expected '(' after move.");
            expr();
            consume(TokenType.COMMA, "Expected ',' in move.");
            expr();
            if (match(TokenType.COMMA)) {
                expr();
            }
            consume(TokenType.RPAREN, "Expected ')' after move args.");
            emit("ABM MOVE statement", t);
            return;
        }

        if (match(TokenType.STEP)) {
            Token t = previous();
            consume(TokenType.LPAREN, "Expected '(' after step.");
            if (!check(TokenType.RPAREN)) expr();
            consume(TokenType.RPAREN, "Expected ')' after step args.");
            emit("ABM STEP statement", t);
            return;
        }

        if (match(TokenType.DESTROY)) {
            Token t = previous();
            consume(TokenType.LPAREN, "Expected '(' after destroy.");
            expr();
            consume(TokenType.RPAREN, "Expected ')' after destroy arg.");
            emit("ABM DESTROY statement", t);
            return;
        }

        Token p = peek();
        throw new ParseException("Expected ABM statement.", p.line(), p.col());
    }

    private void abmCall() {
        if (match(TokenType.NEIGHBORS) || match(TokenType.RAND)) {
            // If we matched one, it is previous()
            consume(TokenType.LPAREN, "Expected '(' after ABM call.");
            if (!check(TokenType.RPAREN)) {
                expr();
                while (match(TokenType.COMMA)) expr();
            }
            consume(TokenType.RPAREN, "Expected ')' after ABM call args.");
            return;
        }
        Token p = peek();
        throw new ParseException("Expected ABM call.", p.line(), p.col());
    }

    // -------------------------
    // Expressions
    // -------------------------
    private void expr() { condExpr(); }

    private void condExpr() {
        orExpr();
        if (match(TokenType.QMARK)) {
            expr();
            consume(TokenType.COLON, "Expected ':' in conditional expression.");
            condExpr();
        }
    }

    private void orExpr() {
        andExpr();
        while (match(TokenType.OROR)) andExpr();
    }

    private void andExpr() {
        eqExpr();
        while (match(TokenType.ANDAND)) eqExpr();
    }

    private void eqExpr() {
        relExpr();
        while (match(TokenType.EQEQ, TokenType.NEQ)) relExpr();
    }

    private void relExpr() {
        addExpr();
        while (match(TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE)) addExpr();
    }

    private void addExpr() {
        mulExpr();
        while (match(TokenType.PLUS, TokenType.MINUS)) mulExpr();
    }

    private void mulExpr() {
        unaryExpr();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) unaryExpr();
    }

    private void unaryExpr() {
        if (match(TokenType.NOT, TokenType.MINUS, TokenType.PLUS, TokenType.AMP, TokenType.STAR)) {
            unaryExpr();
            return;
        }
        primary();
    }

    private void primary() {
        atom();
        // <postfix chain>
        while (true) {
            if (match(TokenType.LPAREN)) {
                // <arg_list_opt>
                if (!check(TokenType.RPAREN)) {
                    expr();
                    while (match(TokenType.COMMA)) expr();
                }
                
                consume(TokenType.RPAREN, "Expected ')' after call.");
                continue;
            }
            if (match(TokenType.LBRACKET)) {
                expr();
                consume(TokenType.RBRACKET, "Expected ']' after index.");
                continue;
            }
            if (match(TokenType.DOT)) {
                consume(TokenType.IDENT, "Expected field name after '.'.");
                continue;
            }
            break;
        }
    }

    private void atom() {
        if (match(TokenType.LPAREN)) {
            expr();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return;
        }

        // <literal>
        if (match(TokenType.INT_LIT, TokenType.FLOAT_LIT, TokenType.CHAR_LIT, TokenType.STRING_LIT,
                TokenType.TRUE, TokenType.FALSE, TokenType.NULL)) {
            return;
        }

        if (match(TokenType.IDENT)) {
            return;
        }

        // <abm_call>
        if (check(TokenType.NEIGHBORS) || check(TokenType.RAND)) {
            abmCall();
            return;
        }

        if (match(TokenType.SELF)) return;

        Token p = peek();
        throw new ParseException("Expected expression atom.", p.line(), p.col());
    }
}