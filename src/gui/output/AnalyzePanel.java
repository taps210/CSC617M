package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.CompileError;
import src.gui.model.Theme;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * Two JTree views side by side: Parse Tree (left) and AST (right).
 */
public class AnalyzePanel extends JPanel implements CompileListener {
    private final PanelHeader header;
    private final DefaultTreeModel parseTreeModel;
    private final DefaultTreeModel astModel;

    public AnalyzePanel() {
        super(new BorderLayout());
        header = new PanelHeader();
        header.set("Trees", Theme.PANEL_HEADER_NEUTRAL, "");

        JTree parseTree = makeTree();
        parseTreeModel = (DefaultTreeModel) parseTree.getModel();

        JTree astTree = makeTree();
        astModel = (DefaultTreeModel) astTree.getModel();

        JPanel parseSection = section("Parse Tree",          scroll(parseTree));
        JPanel astSection   = section("Abstract Syntax Tree", scroll(astTree));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, parseSection, astSection);
        split.setResizeWeight(0.5);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(4);

        add(header, BorderLayout.NORTH);
        add(split,  BorderLayout.CENTER);

        reset();
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (result == null || result.parseTree().isEmpty()) {
            reset();
            return;
        }
        parseTreeModel.setRoot(ParseTreeToTreeModel.from(result.parseTree().get()));

        if (result.ast().isPresent()) {
            astModel.setRoot(AstTreeModel.from(result.ast().get()));
        } else {
            astModel.setRoot(new DefaultMutableTreeNode("(AST not available)"));
        }

        boolean hasParseErrors = result.errors().stream()
                .anyMatch(e -> e.source() == CompileError.Source.PARSER);
        header.set(
            hasParseErrors ? "Parse: FAILED" : "Parse: OK",
            hasParseErrors ? Theme.PANEL_HEADER_FAIL : Theme.PANEL_HEADER_OK,
            ""
        );
    }

    private void reset() {
        header.set("Trees", Theme.PANEL_HEADER_NEUTRAL, "");
        parseTreeModel.setRoot(new DefaultMutableTreeNode("(parse tree will appear here after Run)"));
        astModel.setRoot(new DefaultMutableTreeNode("(AST will appear here after Run)"));
    }

    private static JTree makeTree() {
        JTree t = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("")));
        t.setRootVisible(true);
        t.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return t;
    }

    private static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    private static JPanel section(String title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new SectionDivider(title), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }
}
