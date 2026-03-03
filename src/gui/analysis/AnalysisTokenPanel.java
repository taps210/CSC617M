package src.gui.analysis;

import src.Token;
import src.gui.core.CompileResult;
import src.gui.model.CompileMetrics;
import src.gui.model.TokenColorMap;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Top: colored token table (# | Type | Lexeme | Line | Col).
 * Bottom: MetricsCard with scanner metrics (fixed height for consistent layout).
 */
public class AnalysisTokenPanel extends JPanel {
    private static final String[] COLUMNS = {"#", "Type", "Lexeme", "Line", "Col"};

    private static String formatTimeNs(long ns) {
        if (ns >= 10_000) return String.format("%.2f ms", ns / 1_000_000.0);
        return String.format("%.0f μs", ns / 1_000.0);
    }
    private final JTable table;
    private final DefaultTableModel model;
    private final JPanel metricsPanel;
    private List<Token> tokenList = List.of();

    public AnalysisTokenPanel() {
        setLayout(new BorderLayout());
        metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setPreferredSize(new Dimension(0, AnalysisSourcePanel.METRICS_ROW_HEIGHT));
        metricsPanel.setMinimumSize(new Dimension(0, AnalysisSourcePanel.METRICS_ROW_HEIGHT));
        metricsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, AnalysisSourcePanel.METRICS_ROW_HEIGHT));
        model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (row >= 0 && row < tokenList.size())
                    c.setForeground(TokenColorMap.getColor(tokenList.get(row)));
                return c;
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(metricsPanel, BorderLayout.SOUTH);
    }

    public void populate(CompileResult result) {
        if (result == null) return;
        tokenList = result.tokens();
        model.setRowCount(0);
        int n = 1;
        for (Token t : tokenList) {
            model.addRow(new Object[]{n++, t.type().name(), t.lexeme(), t.line(), t.col()});
        }
        CompileMetrics m = result.metrics();
        metricsPanel.removeAll();
        if (m != null) {
            LinkedHashMap<String, String> entries = new LinkedHashMap<>();
            entries.put("Total Tokens", String.valueOf(m.totalTokens));
            entries.put("Keywords", String.valueOf(m.keywordCount));
            entries.put("Identifiers", String.valueOf(m.identifierCount));
            entries.put("Literals", String.valueOf(m.literalCount));
            entries.put("Operators", String.valueOf(m.operatorCount));
            entries.put("Comments", String.valueOf(m.commentCount));
            entries.put("Errors", String.valueOf(m.scanErrorCount));
            entries.put("Scan Time", formatTimeNs(m.scanTimeNs));
            JScrollPane scroll = new JScrollPane(new MetricsCard("Scanner Metrics", entries));
            scroll.setBorder(null);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            metricsPanel.add(scroll, BorderLayout.CENTER);
        }
        metricsPanel.revalidate();
        metricsPanel.repaint();
    }
}
