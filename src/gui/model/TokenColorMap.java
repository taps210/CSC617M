package src.gui.model;

import src.Token;
import src.TokenType;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static src.gui.model.Theme.*;

/**
 * Single source of truth for token colors. Used by editor syntax highlighter,
 * ScannerOutputPanel, and Analysis token panels.
 */
public final class TokenColorMap {
    private static final Color KEYWORD           = TOKEN_KEYWORD;
    private static final Color ABM_KEYWORD       = TOKEN_ABM_KEYWORD;
    private static final Color TYPE              = TOKEN_TYPE;
    private static final Color DECLARATION       = TOKEN_DECLARATION;
    private static final Color IDENTIFIER        = TOKEN_IDENTIFIER;
    private static final Color LITERAL_NUMERIC   = TOKEN_LITERAL_NUMERIC;
    private static final Color LITERAL_STRING_CHAR = TOKEN_LITERAL_STRING;
    private static final Color BOOLEAN_LIT       = TOKEN_BOOLEAN;
    private static final Color NULL_LIT          = TOKEN_NULL;
    private static final Color OPERATOR_COLOR    = TOKEN_OPERATOR;
    private static final Color PUNCTUATION       = TOKEN_PUNCTUATION;
    private static final Color COMMENT           = TOKEN_COMMENT;
    private static final Color ERROR             = TOKEN_ERROR;

    private static final Set<TokenType> CONTROL_KEYWORDS = Set.of(
            TokenType.IF, TokenType.ELSE, TokenType.WHILE, TokenType.FOR, TokenType.REPEAT, TokenType.UNTIL,
            TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE, TokenType.TRUE, TokenType.FALSE);
    private static final Set<TokenType> ABM_KEYWORDS = Set.of(
            TokenType.SPAWN, TokenType.MOVE, TokenType.STEP, TokenType.NEIGHBORS, TokenType.RAND,
            TokenType.UPDATE, TokenType.DESTROY, TokenType.ZONE, TokenType.PRE, TokenType.POST, TokenType.SELF);
    private static final Set<TokenType> TYPE_KEYWORDS = Set.of(
            TokenType.INT, TokenType.FLOAT, TokenType.CHAR, TokenType.STRING, TokenType.BOOL, TokenType.VOID);
    private static final Set<TokenType> DECLARATION_KEYWORDS = Set.of(
            TokenType.USE, TokenType.CONST, TokenType.TYPE, TokenType.RECORD, TokenType.AGENT, TokenType.WORLD,
            TokenType.READ, TokenType.PRINT, TokenType.MAIN, TokenType.ASSERT);

    private TokenColorMap() {}

    public static Color getColor(TokenType type, String lexeme) {
        if (lexeme != null && LEXEME_MAP.containsKey(lexeme))
            return LEXEME_MAP.get(lexeme);
        switch (type) {
            case INT_LIT:
            case FLOAT_LIT:
                return LITERAL_NUMERIC;
            case STRING_LIT:
            case CHAR_LIT:
                return LITERAL_STRING_CHAR;
            case TRUE:
            case FALSE:
                return BOOLEAN_LIT;
            case NULL:
                return NULL_LIT;
            case IDENT:
                return IDENTIFIER;
            case PLUS: case MINUS: case STAR: case SLASH: case MOD:
            case AMP: case ASSIGN:
            case EQEQ: case NEQ: case LT: case LTE: case GT: case GTE:
            case ANDAND: case OROR: case NOT:
            case QMARK: case COLON:
                return OPERATOR_COLOR;
            case SEMI: case COMMA: case DOT:
            case LPAREN: case RPAREN: case LBRACE: case RBRACE: case LBRACKET: case RBRACKET:
                return PUNCTUATION;
            case EOF:
                return IDENTIFIER;
            default:
                if (CONTROL_KEYWORDS.contains(type)) return KEYWORD;
                if (ABM_KEYWORDS.contains(type)) return ABM_KEYWORD;
                if (TYPE_KEYWORDS.contains(type)) return TYPE;
                if (DECLARATION_KEYWORDS.contains(type)) return DECLARATION;
                return IDENTIFIER;
        }
    }

    private static final Map<String, Color> LEXEME_MAP = new HashMap<>();
    static {
        for (String kw : new String[]{"if", "else", "while", "for", "repeat", "until", "return", "break", "continue", "true", "false"})
            LEXEME_MAP.put(kw, KEYWORD);
        for (String kw : new String[]{"spawn", "move", "step", "destroy", "neighbors", "rand", "update", "zone", "pre", "post", "self"})
            LEXEME_MAP.put(kw, ABM_KEYWORD);
        for (String kw : new String[]{"int", "float", "char", "string", "bool", "void"})
            LEXEME_MAP.put(kw, TYPE);
        for (String kw : new String[]{"use", "const", "type", "record", "agent", "world", "read", "print", "main", "assert"})
            LEXEME_MAP.put(kw, DECLARATION);
        LEXEME_MAP.put("null", NULL_LIT);
    }

    public static Color getColor(Token token) {
        return getColor(token.type(), token.lexeme());
    }
}
