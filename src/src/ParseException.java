package src;

public class ParseException extends RuntimeException {
    public final int line;
    public final int col;

    public ParseException(String message, int line, int col) {
        super("Syntax error at line " + line + " column " + col + ": " + message);
        this.line = line;
        this.col = col;
    }
}