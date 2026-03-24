package src;

public enum TokenType {
    // Special
    EOF,

    // Identifiers + literals
    IDENT, INT_LIT, FLOAT_LIT, STRING_LIT, CHAR_LIT,

    // Keywords
    USE, CONST, TYPE, RECORD, AGENT, WORLD,
    INT, FLOAT, CHAR, STRING, BOOL, VOID,
    IF, ELSE, WHILE, FOR, REPEAT, UNTIL, RETURN, BREAK, CONTINUE,
    READ, PRINT,
    TRUE, FALSE,
    MAIN,
    SPAWN, MOVE, STEP, NEIGHBORS, RAND, UPDATE, DESTROY, ZONE, PRE, POST,
    SELF,
    NULL,
    ASSERT,
    NEW,

    // Operators
    PLUS, MINUS, STAR, SLASH, MOD,
    AMP,
    ASSIGN,
    EQEQ, NEQ, LT, LTE, GT, GTE,
    ANDAND, OROR, NOT,
    QMARK, COLON,
    ARROW,

    // Punctuation
    SEMI, COMMA, DOT,
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET
}
