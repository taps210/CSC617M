package src.parsetree;

import src.Token;

import java.util.List;

/**
 * Single parse tree node: one grammar rule application with optional token (for terminals or location).
 */
public record ParseTreeNode(
        ParseTreeKind kind,
        List<ParseTreeNode> children,
        Token token
) {
    public static ParseTreeNode of(ParseTreeKind kind, List<ParseTreeNode> children) {
        return new ParseTreeNode(kind, children != null ? List.copyOf(children) : List.of(), null);
    }

    public static ParseTreeNode of(ParseTreeKind kind, List<ParseTreeNode> children, Token token) {
        return new ParseTreeNode(kind, children != null ? List.copyOf(children) : List.of(), token);
    }

    public static ParseTreeNode terminal(Token token) {
        return new ParseTreeNode(ParseTreeKind.TERMINAL, List.of(), token);
    }

    public boolean isTerminal() {
        return kind == ParseTreeKind.TERMINAL;
    }
}
