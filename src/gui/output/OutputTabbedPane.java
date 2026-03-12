package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;

import javax.swing.*;
import java.awt.Dimension;

/**
 * Holds Scanner, Parser, Semantic, and Errors tabs. Delegates CompileListener to children.
 */
public class OutputTabbedPane extends JTabbedPane implements CompileListener {
    private final ScannerOutputPanel scannerPanel;
    private final ParserOutputPanel parserPanel;
    private final SemanticOutputPanel semanticPanel;
    private final ErrorsPanel errorsPanel;

    private static final int MIN_PANEL_WIDTH = 320;
    private static final int MIN_PANEL_HEIGHT = 200;

    public OutputTabbedPane() {
        scannerPanel = new ScannerOutputPanel();
        parserPanel = new ParserOutputPanel();
        semanticPanel = new SemanticOutputPanel();
        errorsPanel = new ErrorsPanel();
        scannerPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        parserPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        semanticPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        errorsPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        addTab("Scanner", scannerPanel);
        addTab("Parser", parserPanel);
        addTab("Semantic", semanticPanel);
        addTab("Errors", errorsPanel);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        scannerPanel.onCompileComplete(result);
        parserPanel.onCompileComplete(result);
        semanticPanel.onCompileComplete(result);
        errorsPanel.onCompileComplete(result);
    }

    public ScannerOutputPanel getScannerPanel() { return scannerPanel; }
    public ParserOutputPanel getParserPanel() { return parserPanel; }
    public SemanticOutputPanel getSemanticPanel() { return semanticPanel; }
    public ErrorsPanel getErrorsPanel() { return errorsPanel; }
}
