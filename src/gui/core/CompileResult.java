package src.gui.core;

import src.Token;
import src.gui.model.CompileError;
import src.gui.model.CompileMetrics;

import java.util.List;

/**
 * Full bundle from a compile: source, tokens, parser trace, errors, metrics.
 * Populated only by CompileController; read-only for views.
 */
public record CompileResult(
        String sourceText,
        List<Token> tokens,
        String parserTrace,
        List<CompileError> errors,
        CompileMetrics metrics
) {}
