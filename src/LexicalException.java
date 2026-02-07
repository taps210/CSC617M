package src;

public class LexicalException extends RuntimeException {
    public final int line;
    public final int col;

    public LexicalException(String message, int line, int col) {
        super("Error found in line %d column %d%n%s".formatted(line, col, message));
        this.line = line;
        this.col = col;
    }
}