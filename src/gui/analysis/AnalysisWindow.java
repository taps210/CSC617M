package src.gui.analysis;

import src.gui.core.CompileResult;

import javax.swing.*;
import java.awt.*;

/**
 * Analysis window: three columns (source | token stream | parse), each with viewer + metrics card.
 * Read-only; populates from CompileResult only.
 */
public class AnalysisWindow extends JFrame {
    private final AnalysisSourcePanel sourcePanel;
    private final AnalysisTokenPanel tokenPanel;
    private final AnalysisParserPanel parserPanel;

    public AnalysisWindow() {
        setTitle("Herd — Analysis");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 700);

        sourcePanel = new AnalysisSourcePanel();
        tokenPanel = new AnalysisTokenPanel();
        parserPanel = new AnalysisParserPanel();
        sourcePanel.setMinimumSize(new Dimension(200, 200));
        tokenPanel.setMinimumSize(new Dimension(200, 200));
        parserPanel.setMinimumSize(new Dimension(200, 200));

        JPanel row = new JPanel(new GridLayout(1, 3, 4, 0));
        row.add(sourcePanel);
        row.add(tokenPanel);
        row.add(parserPanel);
        setLayout(new BorderLayout());
        add(row, BorderLayout.CENTER);
    }

    public void open(CompileResult result) {
        if (result == null) return;
        sourcePanel.populate(result.sourceText(), result.metrics());
        tokenPanel.populate(result);
        parserPanel.populate(result);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
