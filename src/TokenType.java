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
    SPAWN, MOVE, STEP, NEIGHBORS, RAND,
    SELF,
    ASSERT,

    // Operators
    PLUS, MINUS, STAR, SLASH, MOD,
    ASSIGN,
    EQEQ, NEQ, LT, LTE, GT, GTE,
    ANDAND, OROR, NOT,
    QMARK, COLON,

    // Punctuation
    SEMI, COMMA, DOT,
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET
}
