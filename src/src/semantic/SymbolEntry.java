package src.semantic;

/**
 * A captured symbol for display in the IDE's Symbol Table view.
 * Collected during semantic analysis via the SymbolTable observer.
 */
public record SymbolEntry(
        String scope,      // "global", "main", "func:<name>", "agent:<name>", "world:<name>"
        String name,
        SymbolTable.Kind kind,
        String typeStr,    // e.g. "int[]", "float(int, bool)", "agent"
        int line
) {}
