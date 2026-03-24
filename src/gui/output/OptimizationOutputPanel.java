package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.Theme;
import src.ir.FunctionIR;
import src.ir.IrFormatter;
import src.ir.IrOptimizer;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.List;

/**
 * Shows before/after IR for each of the three optimization passes
 * (constant folding, branch simplification, dead temp elimination)
 * in three sub-tabs.
 */
public class OptimizationOutputPanel extends JPanel implements CompileListener {

    private static final String PLACEHOLDER = "(Optimization details will appear here after a successful compilation.)";

    // One PassPane per optimization pass
    private final PassPane constantFoldingPane;
    private final PassPane branchSimplificationPane;
    private final PassPane deadTempPane;

    public OptimizationOutputPanel() {
        super(new BorderLayout());

        constantFoldingPane     = new PassPane("Constant Folding");
        branchSimplificationPane = new PassPane("Branch Simplification");
        deadTempPane            = new PassPane("Dead Temp Elimination");

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.addTab("Constant Folding",       constantFoldingPane);
        tabs.addTab("Branch Simplification",  branchSimplificationPane);
        tabs.addTab("Dead Temp Elimination",  deadTempPane);

        add(tabs, BorderLayout.CENTER);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        List<IrOptimizer.OptimizeTrace> traces = result.optimizeTraces();
        if (traces == null || traces.isEmpty()) {
            constantFoldingPane.showPlaceholder();
            branchSimplificationPane.showPlaceholder();
            deadTempPane.showPlaceholder();
            return;
        }

        // Aggregate all functions' IR into before/after strings per pass
        StringBuilder cfBefore = new StringBuilder();
        StringBuilder cfAfter  = new StringBuilder();
        StringBuilder bsBefore = new StringBuilder();
        StringBuilder bsAfter  = new StringBuilder();
        StringBuilder dtBefore = new StringBuilder();
        StringBuilder dtAfter  = new StringBuilder();

        for (IrOptimizer.OptimizeTrace t : traces) {
            cfBefore.append(format(t.original())).append("\n");
            cfAfter.append(format(t.afterConstantFolding())).append("\n");
            bsBefore.append(format(t.afterConstantFolding())).append("\n");
            bsAfter.append(format(t.afterBranchSimplification())).append("\n");
            dtBefore.append(format(t.afterBranchSimplification())).append("\n");
            dtAfter.append(format(t.afterDeadTempElimination())).append("\n");
        }

        // Compute stats
        int folded = traces.stream().mapToInt(IrOptimizer.OptimizeTrace::foldedCount).sum();
        int simplified = traces.stream().mapToInt(IrOptimizer.OptimizeTrace::simplifiedBranchCount).sum();
        int removed = traces.stream().mapToInt(IrOptimizer.OptimizeTrace::removedDeadTempCount).sum();

        constantFoldingPane.show(cfBefore.toString(), cfAfter.toString(),
                folded + " constant(s) folded");
        branchSimplificationPane.show(bsBefore.toString(), bsAfter.toString(),
                simplified + " branch(es) simplified");
        deadTempPane.show(dtBefore.toString(), dtAfter.toString(),
                removed + " dead temp(s) removed");
    }

    private static String format(FunctionIR f) {
        if (f == null) return "";
        return IrFormatter.formatFunctionIR(f);
    }

    // -------------------------------------------------------------------------

    /**
     * A single pass sub-panel: header, then left=Before / right=After split pane.
     */
    private static class PassPane extends JPanel {
        private final PanelHeader header;
        private final JTextPane beforePane;
        private final JTextPane afterPane;
        private final String passName;

        PassPane(String passName) {
            super(new BorderLayout());
            this.passName = passName;

            header = new PanelHeader();
            header.set(passName, Theme.PANEL_HEADER_NEUTRAL, "");
            add(header, BorderLayout.NORTH);

            beforePane = makeTextPane();
            afterPane  = makeTextPane();

            JPanel beforePanel = labeled("Before", new JScrollPane(beforePane));
            JPanel afterPanel  = labeled("After",  new JScrollPane(afterPane));

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, beforePanel, afterPanel);
            split.setResizeWeight(0.5);
            split.setBorder(BorderFactory.createEmptyBorder());
            add(split, BorderLayout.CENTER);

            showPlaceholder();
        }

        void showPlaceholder() {
            header.set(passName, Theme.PANEL_HEADER_NEUTRAL, "");
            renderIR(beforePane, PLACEHOLDER, true);
            renderIR(afterPane,  PLACEHOLDER, true);
        }

        void show(String before, String after, String stats) {
            header.set(passName, Theme.PANEL_HEADER_OK, stats);
            renderIR(beforePane, before.isEmpty() ? PLACEHOLDER : before, before.isEmpty());
            renderIR(afterPane,  after.isEmpty()  ? PLACEHOLDER : after,  after.isEmpty());
            SwingUtilities.invokeLater(() -> {
                beforePane.setCaretPosition(0);
                afterPane.setCaretPosition(0);
            });
        }

        private static JTextPane makeTextPane() {
            JTextPane tp = new JTextPane();
            tp.setEditable(false);
            tp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            return tp;
        }

        private static JPanel labeled(String label, JComponent comp) {
            JPanel p = new JPanel(new BorderLayout());
            JLabel lbl = new JLabel(label);
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            p.add(lbl, BorderLayout.NORTH);
            p.add(comp, BorderLayout.CENTER);
            return p;
        }

        private static void renderIR(JTextPane pane, String text, boolean placeholder) {
            StyledDocument doc = pane.getStyledDocument();
            try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}

            if (placeholder) {
                SimpleAttributeSet dim = attr(Theme.LINE_NUMBER_FG);
                try { doc.insertString(0, text, dim); } catch (BadLocationException ignored) {}
                return;
            }

            SimpleAttributeSet aDefault = attr(Theme.EDITOR_FG);
            SimpleAttributeSet aFunc    = attr(Theme.TOKEN_DECLARATION);
            SimpleAttributeSet aLabel   = attr(Theme.TOKEN_ABM_KEYWORD);
            SimpleAttributeSet aControl = attr(Theme.TOKEN_KEYWORD);
            SimpleAttributeSet aCall    = attr(Theme.TOKEN_LITERAL_STRING);
            SimpleAttributeSet aType    = attr(Theme.TOKEN_TYPE);

            for (String line : text.split("\n", -1)) {
                String t = line.trim();
                SimpleAttributeSet style;
                if      (t.startsWith("function "))                        style = aFunc;
                else if (t.endsWith(":") && !t.contains(" "))              style = aLabel;
                else if (t.startsWith("goto ")   || t.startsWith("if ")
                      || t.equals("return")      || t.startsWith("return ")
                      || t.startsWith("param ")  || t.startsWith("print ")
                      || t.startsWith("read "))                            style = aControl;
                else if (t.contains("call "))                              style = aCall;
                else if (t.contains("new array") || t.contains("new "))   style = aType;
                else                                                       style = aDefault;

                try { doc.insertString(doc.getLength(), line + "\n", style); }
                catch (BadLocationException ignored) {}
            }
        }

        private static SimpleAttributeSet attr(Color fg) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setFontFamily(a, Font.MONOSPACED);
            StyleConstants.setFontSize(a, 13);
            StyleConstants.setForeground(a, fg);
            return a;
        }
    }
}
