package src;

/**
 * Record of a single lexical error (line, column, message).
 * Used when the scanner runs in error-collection mode instead of throwing on first error.
 */
public record LexicalErrorRecord(int line, int col, String message) {
    public String format() {
        return "Error found in line %d column %d%n%s".formatted(line, col, message);
    }
}
