package src.gui.core;

import src.Token;
import static src.Ast.*;
import src.gui.model.CompileError;
import src.gui.model.CompileMetrics;
import src.ir.IrOptimizer;
import src.parsetree.ParseTreeNode;
import src.semantic.SymbolEntry;

import java.util.List;
import java.util.Optional;

/**
 * Full bundle from a compile: source, tokens, parser trace, errors, metrics, optional AST, optional parse tree.
 * Populated only by CompileController; read-only for views.
 */
public record CompileResult(
        String sourceText,
        List<Token> tokens,
        String parserTrace,
        List<CompileError> errors,
        CompileMetrics metrics,
        Optional<ProgramNode> ast,
        Optional<ParseTreeNode> parseTree,
        Optional<String> irText,
        Optional<String> cfgText,
        Optional<String> interpreterOutput,
        List<SymbolEntry> symbolEntries,
        List<IrOptimizer.OptimizeTrace> optimizeTraces
) {
    /** Legacy constructor without AST or parse tree (uses Optional.empty()). */
    public CompileResult(String sourceText, List<Token> tokens, String parserTrace,
                         List<CompileError> errors, CompileMetrics metrics) {
        this(sourceText, tokens, parserTrace, errors, metrics, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of());
    }
}
