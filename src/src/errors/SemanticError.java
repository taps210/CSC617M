package src.errors;

import src.gui.model.CompileError;

/**
 * One semantic error (line, column, message). Produced by semantic analysis.
 * Convert to {@link CompileError} with source SEMANTIC when reporting to the GUI.
 */
public record SemanticError(int line, int col, String message) {
    public CompileError toCompileError() {
        return new CompileError(line, col, message, CompileError.Source.SEMANTIC, CompileError.Severity.ERROR);
    }
}
