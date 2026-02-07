package src;

public record Token(
        TokenType type,
        String lexeme,
        Object literal,
        int line,
        int col
) {
    @Override
    public String toString() {
        if (literal != null) {
            return "%s Token \"%s\" (%s) found in line %d column %d"
                    .formatted(type, lexeme, literal, line, col);
        }
        return "%s Token \"%s\" found in line %d column %d"
                .formatted(type, lexeme, line, col);
    }
}
