package src.gui.output;

import src.parsetree.ParseTreeNode;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Builds a Swing tree node from a ParseTreeNode for parse tree visualization.
 */
final class ParseTreeToTreeModel {

    static DefaultMutableTreeNode from(ParseTreeNode root) {
        return toSwingNode(root);
    }

    private static DefaultMutableTreeNode toSwingNode(ParseTreeNode n) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label(n));
        for (ParseTreeNode c : n.children()) {
            node.add(toSwingNode(c));
        }
        return node;
    }

    private static String label(ParseTreeNode n) {
        if (n.isTerminal()) {
            if (n.token() != null) {
                String lex = n.token().lexeme();
                if (lex != null && !lex.isEmpty()) return n.token().type() + " \"" + lex + "\"";
                return String.valueOf(n.token().type());
            }
            return "TERMINAL";
        }
        String kind = n.kind().name();
        if (n.token() != null) return kind + " (L" + n.token().line() + ")";
        return kind;
    }
}
