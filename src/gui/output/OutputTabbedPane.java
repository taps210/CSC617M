package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;

import javax.swing.*;
import java.awt.Dimension;

/**
 * Holds Scanner, Parser, and Errors tabs. Delegates CompileListener to children.
 */
public class OutputTabbedPane extends JTabbedPane implements CompileListener {
    private final ScannerOutputPanel scannerPanel;
    private final ParserOutputPanel parserPanel;
    private final ErrorsPanel errorsPanel;

    private static final int MIN_PANEL_WIDTH = 320;
    private static final int MIN_PANEL_HEIGHT = 200;

    public OutputTabbedPane() {
        scannerPanel = new ScannerOutputPanel();
        parserPanel = new ParserOutputPanel();
        errorsPanel = new ErrorsPanel();
        scannerPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        parserPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        errorsPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        addTab("Scanner", scannerPanel);
        addTab("Parser", parserPanel);
        addTab("Errors", errorsPanel);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        scannerPanel.onCompileComplete(result);
        parserPanel.onCompileComplete(result);
        errorsPanel.onCompileComplete(result);
    }

    public ScannerOutputPanel getScannerPanel() { return scannerPanel; }
    public ParserOutputPanel getParserPanel() { return parserPanel; }
    public ErrorsPanel getErrorsPanel() { return errorsPanel; }
}
