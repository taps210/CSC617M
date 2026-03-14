package src.semantic;

import static src.Ast.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scope stack for name resolution. Supports define(name, kind, type) and resolve(name).
 */
public class SymbolTable {
    public enum Kind { VARIABLE, CONSTANT, TYPE, FUNCTION, AGENT, WORLD }

    public static final class Symbol {
        public final String name;
        public final Kind kind;
        public final DataTypeNode type;
        public final SourceSpan declarationSpan;
        /** For functions: param types; for agents: unused. */
        public final List<DataTypeNode> paramTypes;

        public Symbol(String name, Kind kind, DataTypeNode type, SourceSpan declarationSpan, List<DataTypeNode> paramTypes) {
            this.name = name;
            this.kind = kind;
            this.type = type;
            this.declarationSpan = declarationSpan;
            this.paramTypes = paramTypes != null ? List.copyOf(paramTypes) : List.of();
        }

        public static Symbol variable(String name, DataTypeNode type, SourceSpan span) {
            return new Symbol(name, Kind.VARIABLE, type, span, null);
        }
        public static Symbol constant(String name, DataTypeNode type, SourceSpan span) {
            return new Symbol(name, Kind.CONSTANT, type, span, null);
        }
        public static Symbol type(String name, DataTypeNode type, SourceSpan span) {
            return new Symbol(name, Kind.TYPE, type, span, null);
        }
        public static Symbol function(String name, DataTypeNode returnType, List<DataTypeNode> paramTypes, SourceSpan span) {
            return new Symbol(name, Kind.FUNCTION, returnType, span, paramTypes);
        }
        public static Symbol agent(String name, SourceSpan span) {
            return new Symbol(name, Kind.AGENT, null, span, null);
        }
        public static Symbol world(String name, SourceSpan span) {
            return new Symbol(name, Kind.WORLD, null, span, null);
        }
    }

    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    public void popScope() {
        if (!scopes.isEmpty()) scopes.pop();
    }

    /** Define a symbol in the current scope. Returns false if already defined in this scope (duplicate). */
    public boolean define(Symbol sym) {
        if (scopes.isEmpty()) return false;
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(sym.name)) return false;
        current.put(sym.name, sym);
        return true;
    }

    /** Resolve name from innermost to outermost scope. Returns null if not found. */
    public Symbol resolve(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol s = scope.get(name);
            if (s != null) return s;
        }
        return null;
    }

    /** Check if name is defined in current scope only (for duplicate check). */
    public boolean definedInCurrentScope(String name) {
        if (scopes.isEmpty()) return false;
        return scopes.peek().containsKey(name);
    }
}
