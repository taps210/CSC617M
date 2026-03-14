package src.errors;

/**
 * Thrown by the parser when a syntax error is encountered (unexpected token, missing token, etc.).
 */
public class ParseException extends RuntimeException {
    private final int line;
    private final int col;

    public ParseException(String message, int line, int col) {
        super("Syntax error at line " + line + " column " + col + ": " + message);
        this.line = line;
        this.col = col;
    }

    public int line() { return line; }
    public int col()  { return col; }
}
