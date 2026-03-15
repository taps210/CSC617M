package src.semantic;

import src.errors.SemanticError;
import java.util.List;

/** Full output of semantic analysis: errors + captured symbol table entries. */
public record SemanticResult(
        List<SemanticError> errors,
        List<SymbolEntry> symbols
) {}
