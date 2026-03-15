package src.semantic;

import static src.Ast.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Scope stack for name resolution. Supports define(name, kind, type) and resolve(name).
 * Each scope can carry an optional label (e.g. "global", "func:foo", "agent:Bee").
 * Unnamed scopes (plain pushScope()) inherit the parent label, so local variables
 * inside a function correctly report the function's scope name.
 */
public class SymbolTable {
    public enum Kind { VARIABLE, CONSTANT, TYPE, FUNCTION, AGENT, WORLD }

    public static final class Symbol {
        public final String name;
        public final Kind kind;
        public final DataTypeNode type;
        public final SourceSpan declarationSpan;
        /** For functions: param types; for others: empty. */
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

    private final Deque<Map<String, Symbol>> scopes      = new ArrayDeque<>();
    private final Deque<String>              scopeLabels = new ArrayDeque<>();
    private Consumer<Symbol> onDefine;

    /** Register a listener that fires on every successful define(). */
    public void setOnDefine(Consumer<Symbol> listener) { this.onDefine = listener; }

    /** Push an unnamed scope — inherits the parent scope's label. */
    public void pushScope() {
        String inherited = scopeLabels.isEmpty() ? "global" : scopeLabels.peek();
        pushScope(inherited);
    }

    /** Push a named scope (e.g. "func:foo", "agent:Bee", "global"). */
    public void pushScope(String label) {
        scopes.push(new HashMap<>());
        scopeLabels.push(label);
    }

    public void popScope() {
        if (!scopes.isEmpty())      scopes.pop();
        if (!scopeLabels.isEmpty()) scopeLabels.pop();
    }

    /** Returns the label of the innermost scope, or "global" if the stack is empty. */
    public String currentScopeName() {
        return scopeLabels.isEmpty() ? "global" : scopeLabels.peek();
    }

    /** Define a symbol in the current scope. Returns false if already defined in this scope (duplicate). */
    public boolean define(Symbol sym) {
        if (scopes.isEmpty()) return false;
        Map<String, Symbol> current = scopes.peek();
        if (current.containsKey(sym.name)) return false;
        current.put(sym.name, sym);
        if (onDefine != null) onDefine.accept(sym);
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
