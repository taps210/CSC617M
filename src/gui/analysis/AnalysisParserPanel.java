package src.gui.analysis;

import src.gui.core.CompileResult;
import src.gui.model.CompileMetrics;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.LinkedHashMap;

/**
 * Tabbed view: Trace (parser trace text) | AST (JTree from ProgramNode) | Parse tree (JTree from ParseTreeNode). Bottom: MetricsCard with parser metrics.
 */
public class AnalysisParserPanel extends JPanel {
    private static String formatTimeNs(long ns) {
        if (ns >= 10_000) return String.format("%.2f ms", ns / 1_000_000.0);
        return String.format("%.0f μs", ns / 1_000.0);
    }
    private final JTextArea traceArea;
    private final JTree astTree;
    private final JTree parseTree;
    private final JPanel metricsPanel;

    public AnalysisParserPanel() {
        setLayout(new BorderLayout());
        traceArea = new JTextArea();
        traceArea.setEditable(false);
        traceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        astTree = new JTree();
        astTree.setShowsRootHandles(true);
        astTree.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        parseTree = new JTree();
        parseTree.setShowsRootHandles(true);
        parseTree.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Trace", new JScrollPane(traceArea));
        tabs.addTab("AST", new JScrollPane(astTree));
        tabs.addTab("Parse tree", new JScrollPane(parseTree));
        add(tabs, BorderLayout.CENTER);
        metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setPreferredSize(new Dimension(0, AnalysisSourcePanel.METRICS_ROW_HEIGHT));
        metricsPanel.setMinimumSize(new Dimension(0, AnalysisSourcePanel.METRICS_ROW_HEIGHT));
        metricsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, AnalysisSourcePanel.METRICS_ROW_HEIGHT));
        add(metricsPanel, BorderLayout.SOUTH);
    }

    public void populate(CompileResult result) {
        if (result == null) return;
        traceArea.setText(result.parserTrace() != null ? result.parserTrace() : "");
        traceArea.setCaretPosition(0);
        if (result.ast().isPresent()) {
            DefaultTreeModel model = AstTreeModel.from(result.ast().get());
            astTree.setModel(model);
            for (int i = 0; i < astTree.getRowCount(); i++) astTree.expandRow(i);
        } else {
            astTree.setModel(new DefaultTreeModel(new javax.swing.tree.DefaultMutableTreeNode("AST not available (parse failed or no parse run).")));
        }
        if (result.parseTree().isPresent()) {
            DefaultTreeModel ptModel = ParseTreeToTreeModel.from(result.parseTree().get());
            parseTree.setModel(ptModel);
            for (int i = 0; i < parseTree.getRowCount(); i++) parseTree.expandRow(i);
        } else {
            parseTree.setModel(new DefaultTreeModel(new javax.swing.tree.DefaultMutableTreeNode("Parse tree not available (parse failed or no parse run).")));
        }
        CompileMetrics m = result.metrics();
        metricsPanel.removeAll();
        if (m != null) {
            LinkedHashMap<String, String> entries = new LinkedHashMap<>();
            entries.put("Constructs", String.valueOf(m.parseNodeCount));
            entries.put("Errors", String.valueOf(m.parseErrorCount));
            entries.put("Warnings", String.valueOf(m.parseWarningCount));
            entries.put("Parse Time", formatTimeNs(m.parseTimeNs));
            JScrollPane scroll = new JScrollPane(new MetricsCard("Parser Metrics", entries));
            scroll.setBorder(null);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            metricsPanel.add(scroll, BorderLayout.CENTER);
        }
        metricsPanel.revalidate();
        metricsPanel.repaint();
    }
}
