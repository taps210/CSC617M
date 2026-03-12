package src.gui.analysis;

import src.parsetree.ParseTreeNode;
import src.parsetree.ParseTreeKind;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

/**
 * Builds a Swing DefaultTreeModel from a ParseTreeNode for parse tree visualization.
 */
public final class ParseTreeToTreeModel {

    public static DefaultTreeModel from(ParseTreeNode root) {
        return new DefaultTreeModel(toSwingNode(root));
    }

    private static DefaultMutableTreeNode toSwingNode(ParseTreeNode n) {
        String label = label(n);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);
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
        if (n.token() != null) {
            int line = n.token().line();
            return kind + " (L" + line + ")";
        }
        return kind;
    }
}
