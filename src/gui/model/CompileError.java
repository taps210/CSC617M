package src.gui.model;

/**
 * Unified error for both lexical and parse errors.
 * Used by ErrorsPanel and ErrorHighlighter.
 */
public record CompileError(
        int line,
        int col,
        String message,
        Source source,
        Severity severity
) {
    public enum Source { LEXER, PARSER, SEMANTIC, IR, RUNTIME }
    public enum Severity { ERROR, WARN }
}
