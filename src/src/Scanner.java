package src;

import src.errors.LexicalErrorRecord;

import java.util.*;

public final class Scanner {
    private final String src;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("use", TokenType.USE),
            Map.entry("const", TokenType.CONST),
            Map.entry("type", TokenType.TYPE),
            Map.entry("record", TokenType.RECORD),
            Map.entry("agent", TokenType.AGENT),
            Map.entry("world", TokenType.WORLD),

            Map.entry("int", TokenType.INT),
            Map.entry("float", TokenType.FLOAT),
            Map.entry("char", TokenType.CHAR),
            Map.entry("string", TokenType.STRING),
            Map.entry("bool", TokenType.BOOL),
            Map.entry("void", TokenType.VOID),

            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("while", TokenType.WHILE),
            Map.entry("for", TokenType.FOR),
            Map.entry("repeat", TokenType.REPEAT),
            Map.entry("until", TokenType.UNTIL),
            Map.entry("return", TokenType.RETURN),
            Map.entry("break", TokenType.BREAK),
            Map.entry("continue", TokenType.CONTINUE),

            Map.entry("read", TokenType.READ),
            Map.entry("print", TokenType.PRINT),

            Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE),

            Map.entry("main", TokenType.MAIN),

            Map.entry("spawn", TokenType.SPAWN),
            Map.entry("move", TokenType.MOVE),
            Map.entry("step", TokenType.STEP),
            Map.entry("neighbors", TokenType.NEIGHBORS),
            Map.entry("rand", TokenType.RAND),
            Map.entry("update", TokenType.UPDATE),
            Map.entry("destroy", TokenType.DESTROY),
            Map.entry("zone", TokenType.ZONE),
            Map.entry("pre", TokenType.PRE),
            Map.entry("post", TokenType.POST),
            Map.entry("self", TokenType.SELF),
            Map.entry("null", TokenType.NULL),
            Map.entry("assert", TokenType.ASSERT)
    );

    // Stores entire program text in src
    public Scanner(String source) {
        this.src = Objects.requireNonNull(source);
    }

    // Returns list of tokens including EOF
    public List<Token> tokenizeAll(boolean printRecoveryMessages) {
        return tokenizeAll(printRecoveryMessages, null);
    }

    /**
     * When errorCollector is non-null, recoverable lexical errors (&, |, unknown symbol)
     * are added to the list and scanning continues. Non-recoverable errors (e.g. unterminated string) still throw.
     */
    public List<Token> tokenizeAll(boolean printRecoveryMessages, List<LexicalErrorRecord> errorCollector) {
        var out = new ArrayList<Token>();
        try {
            while (true) {
                var t = nextToken(printRecoveryMessages, errorCollector);
                out.add(t);
                if (t.type() == TokenType.EOF) break;
            }
        } catch (LexicalErrorRecord.ScanAbortedException e) {
            if (errorCollector != null && e.getError() != null) errorCollector.add(e.getError());
            throw e;
        }
        return out;
    }

    // Core function that returns next token per call
    public Token nextToken(boolean printRecoveryMessages) {
        return nextToken(printRecoveryMessages, null);
    }

    private Token nextToken(boolean printRecoveryMessages, List<LexicalErrorRecord> errorCollector) {
        skipWhitespaceAndComments();

        if (isAtEnd()) {
            return new Token(TokenType.EOF, "", null, line, col);
        }

        int startLine = line;
        int startCol = col;
        char c = peek();

        // Identifiers / keywords
        if (isAlpha(c) || c == '_') {
            String lex = readIdentifier();
            TokenType tt = KEYWORDS.getOrDefault(lex, TokenType.IDENT);
            return new Token(tt, lex, null, startLine, startCol);
        }

        // Numbers: int or float; also handle "numbers with letters within"
        if (isDigit(c) || (c == '.' && isDigit(peekNext()))) {
            return readNumberWithRecovery(printRecoveryMessages);
        }

        // String literal
        if (c == '"') {
            return readStringLiteral();
        }

        // Char literal
        if (c == '\'') {
            return readCharLiteral();
        }

        // Operators / punctuation (multi-char first)
        return switch (c) {
            case '=' -> {
                advance();
                if (match('=')) yield new Token(TokenType.EQEQ, "==", null, startLine, startCol);
                yield new Token(TokenType.ASSIGN, "=", null, startLine, startCol);
            }
            case '!' -> {
                advance();
                if (match('=')) yield new Token(TokenType.NEQ, "!=", null, startLine, startCol);
                yield new Token(TokenType.NOT, "!", null, startLine, startCol);
            }
            case '<' -> {
                advance();
                if (match('=')) yield new Token(TokenType.LTE, "<=", null, startLine, startCol);
                yield new Token(TokenType.LT, "<", null, startLine, startCol);
            }
            case '>' -> {
                advance();
                if (match('=')) yield new Token(TokenType.GTE, ">=", null, startLine, startCol);
                yield new Token(TokenType.GT, ">", null, startLine, startCol);
            }
            case '&' -> {
                advance();
                if (match('&')) yield new Token(TokenType.ANDAND, "&&", null, startLine, startCol);
                yield new Token(TokenType.AMP, "&", null, startLine, startCol);
            }
            case '|' -> {
                advance();
                if (match('|')) yield new Token(TokenType.OROR, "||", null, startLine, startCol);
                if (errorCollector != null) {
                    errorCollector.add(new LexicalErrorRecord(startLine, startCol, "Unknown symbol \"|\" (did you mean \"||\"?)"));
                    yield nextToken(printRecoveryMessages, errorCollector);
                }
                throw new LexicalErrorRecord.ScanAbortedException(new LexicalErrorRecord(startLine, startCol, "Unknown symbol \"|\" (did you mean \"||\"?)"));
            }

            case '+' -> { advance(); yield new Token(TokenType.PLUS, "+", null, startLine, startCol); }
            case '-' -> { advance(); yield new Token(TokenType.MINUS, "-", null, startLine, startCol); }
            case '*' -> { advance(); yield new Token(TokenType.STAR, "*", null, startLine, startCol); }
            case '/' -> { advance(); yield new Token(TokenType.SLASH, "/", null, startLine, startCol); }
            case '%' -> { advance(); yield new Token(TokenType.MOD, "%", null, startLine, startCol); }

            case '?' -> { advance(); yield new Token(TokenType.QMARK, "?", null, startLine, startCol); }
            case ':' -> { advance(); yield new Token(TokenType.COLON, ":", null, startLine, startCol); }

            case ';' -> { advance(); yield new Token(TokenType.SEMI, ";", null, startLine, startCol); }
            case ',' -> { advance(); yield new Token(TokenType.COMMA, ",", null, startLine, startCol); }
            case '.' -> { advance(); yield new Token(TokenType.DOT, ".", null, startLine, startCol); }

            case '(' -> { advance(); yield new Token(TokenType.LPAREN, "(", null, startLine, startCol); }
            case ')' -> { advance(); yield new Token(TokenType.RPAREN, ")", null, startLine, startCol); }
            case '{' -> { advance(); yield new Token(TokenType.LBRACE, "{", null, startLine, startCol); }
            case '}' -> { advance(); yield new Token(TokenType.RBRACE, "}", null, startLine, startCol); }
            case '[' -> { advance(); yield new Token(TokenType.LBRACKET, "[", null, startLine, startCol); }
            case ']' -> { advance(); yield new Token(TokenType.RBRACKET, "]", null, startLine, startCol); }

            default -> {
                advance();
                if (errorCollector != null) {
                    errorCollector.add(new LexicalErrorRecord(startLine, startCol, "Unknown symbol \"%s\"".formatted(c)));
                    yield nextToken(printRecoveryMessages, errorCollector);
                }
                throw new LexicalErrorRecord.ScanAbortedException(new LexicalErrorRecord(startLine, startCol, "Unknown symbol \"%s\"".formatted(c)));
            }
        };
    }

    // ---------- Core readers ----------

    private Token readNumberWithRecovery(boolean printRecoveryMessages) {
        int startLine = line;
        int startCol = col;

        int start = pos;
        boolean seenDot = false;

        // Allow leading '.' for floats like .5
        if (peek() == '.') {
            seenDot = true;
            advance(); // consume '.'
            while (isDigit(peek())) advance();
        } else {
            while (isDigit(peek())) advance();
            if (peek() == '.') {
                seenDot = true;
                advance(); // consume '.'
                while (isDigit(peek())) advance();
            }
        }

        String numberLexeme = src.substring(start, pos);

        // If the next char is a letter/underscore, it's a lexical error ("numbers with letters within")
        if (isAlpha(peek()) || peek() == '_') {
            char bad = peek();
            // Recover by consuming the bad character and continuing scanning (so remaining becomes an IDENT later)
            if (printRecoveryMessages) {
                System.out.println("Error found in line %d column %d".formatted(startLine, startCol));
                System.out.println("Recovering: Replacing symbol \"%s\" at column %d with space"
                        .formatted(bad, col));
            }
            advance(); // skip the offending character
            // Return the number token we managed to read
        }

        if (seenDot) {
            // literal as Double
            Double val = Double.parseDouble(normalizeFloat(numberLexeme));
            return new Token(TokenType.FLOAT_LIT, numberLexeme, val, startLine, startCol);
        } else {
            Integer val = Integer.parseInt(numberLexeme);
            return new Token(TokenType.INT_LIT, numberLexeme, val, startLine, startCol);
        }
    }

    private Token readStringLiteral() {
        int startLine = line;
        int startCol = col;

        advance(); // opening "
        var sb = new StringBuilder();

        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\n') {
                throw new LexicalErrorRecord.ScanAbortedException(new LexicalErrorRecord(startLine, startCol, "Unterminated string constant"));
            }
            if (c == '\\') {
                if (isAtEnd()) throw new LexicalErrorRecord.ScanAbortedException(new LexicalErrorRecord(startLine, startCol, "Unterminated string constant"));
                char esc = advance();
                sb.append(switch (esc) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '\\' -> '\\';
                    case '"' -> '"';
                    case '\'' -> '\'';
                    default -> esc; // keep unknown escapes as-is (simple)
                });
            } else {
                sb.append(c);
            }
        }

        if (isAtEnd()) throw new LexicalErrorRecord.ScanAbortedException(new LexicalErrorRecord(startLine, startCol, "Unterminated string constant"));

        advance(); // closing "
        String literal = sb.toString();
        String fullLexeme = "\"" + escapeForLexeme(literal) + "\"";
        return new Token(TokenType.STRING_LIT, fullLexeme, literal, startLine, startCol);
    }

    private Token readCharLiteral() {
        int startLine = line;
        int startCol = col;

        advance(); // opening '
        if (isAtEnd() || peek() == '\n') {
            throw new LexicalErrorRecord.ScanAbortedException(new LexicalErrorRecord(startLine, startCol, "Unterminated character constant"));
        }

        char value;
        char c = advance();
        if (c == '\\') {
            if (isAtEnd()) throw new LexicalErrorRecord.ScanAbortedException(new LexicalErrorRecord(startLine, startCol, "Unterminated character constant"));
            char esc = advance();
            value = switch (esc) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case '\\' -> '\\';
                case '"' -> '"';
                case '\'' -> '\'';
                default -> esc;
            };
        } else {
            value = c;
        }

        if (isAtEnd() || peek() != '\'') {
            throw new LexicalErrorRecord.ScanAbortedException(new LexicalErrorRecord(startLine, startCol, "Unterminated character constant"));
        }

        advance(); // closing '
        String lexeme = "'" + (value == '\n' ? "\\n" : value) + "'";
        return new Token(TokenType.CHAR_LIT, lexeme, value, startLine, startCol);
    }

    private String readIdentifier() {
        int start = pos;
        advance(); // first char
        while (isAlphaNumeric(peek()) || peek() == '_') advance();
        return src.substring(start, pos);
    }

    // ---------- Skipping whitespace/comments ----------

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();

            // Whitespace
            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
                continue;
            }

            // Newline
            if (c == '\n') {
                advance(); // advance() updates line/col
                continue;
            }

            // Line comment //
            if (c == '/' && peekNext() == '/') {
                advance(); advance(); // consume //
                while (!isAtEnd() && peek() != '\n') advance();
                continue;
            }

            // Block comment /* ... */
            if (c == '/' && peekNext() == '*') {
                advance(); advance(); // consume /*
                while (!isAtEnd()) {
                    if (peek() == '*' && peekNext() == '/') {
                        advance(); advance(); // consume */
                        break;
                    }
                    advance();
                }
                continue;
            }

            break;
        }
    }

    // ---------- Helpers ----------

    private boolean isAtEnd() {
        return pos >= src.length();
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return src.charAt(pos);
    }

    private char peekNext() {
        if (pos + 1 >= src.length()) return '\0';
        return src.charAt(pos + 1);
    }

    private char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (src.charAt(pos) != expected) return false;
        advance();
        return true;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private static String escapeForLexeme(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String normalizeFloat(String lexeme) {
        if (lexeme.startsWith(".")) return "0" + lexeme;
        return lexeme;
    }
}
