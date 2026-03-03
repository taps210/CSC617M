package src.gui.analysis;

import src.gui.core.CompileResult;
import src.gui.model.CompileMetrics;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;

/**
 * Top: read-only source viewer (same syntax coloring as editor).
 * Bottom: MetricsCard with source metrics (fixed height for consistent layout).
 */
public class AnalysisSourcePanel extends JPanel {
    /** Fixed height for metrics row so all three columns are leveled. */
    public static final int METRICS_ROW_HEIGHT = 220;
    private final JTextPane sourceView;
    private final JPanel metricsPanel;

    public AnalysisSourcePanel() {
        setLayout(new BorderLayout());
        sourceView = new JTextPane();
        sourceView.setEditable(false);
        sourceView.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(sourceView), BorderLayout.CENTER);
        metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setPreferredSize(new Dimension(0, METRICS_ROW_HEIGHT));
        metricsPanel.setMinimumSize(new Dimension(0, METRICS_ROW_HEIGHT));
        metricsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, METRICS_ROW_HEIGHT));
        add(metricsPanel, BorderLayout.SOUTH);
    }

    public void populate(String sourceText, CompileMetrics metrics) {
        sourceView.setText(sourceText != null ? sourceText : "");
        sourceView.setCaretPosition(0);
        metricsPanel.removeAll();
        if (metrics != null) {
            LinkedHashMap<String, String> entries = new LinkedHashMap<>();
            entries.put("Characters", String.valueOf(metrics.charCount));
            entries.put("Words", String.valueOf(metrics.wordCount));
            entries.put("Total Lines", String.valueOf(metrics.totalLines));
            entries.put("Code Lines", String.valueOf(metrics.codeLines));
            entries.put("Blank Lines", String.valueOf(metrics.blankLines));
            entries.put("Comment Lines", String.valueOf(metrics.commentLines));
            JScrollPane scroll = new JScrollPane(new MetricsCard("Source Metrics", entries));
            scroll.setBorder(null);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            metricsPanel.add(scroll, BorderLayout.CENTER);
        }
        metricsPanel.revalidate();
        metricsPanel.repaint();
    }
}
