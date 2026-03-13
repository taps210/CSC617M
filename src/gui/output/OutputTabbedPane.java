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
    private final IrOutputPanel irPanel;
    private final InterpreterOutputPanel interpreterPanel;

    private static final int MIN_PANEL_WIDTH = 320;
    private static final int MIN_PANEL_HEIGHT = 200;

    public OutputTabbedPane() {
        scannerPanel = new ScannerOutputPanel();
        parserPanel = new ParserOutputPanel();
        semanticPanel = new SemanticOutputPanel();
        errorsPanel = new ErrorsPanel();
        irPanel = new IrOutputPanel();
        interpreterPanel = new InterpreterOutputPanel();
        scannerPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        parserPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        semanticPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        errorsPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        irPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        interpreterPanel.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        addTab("Scanner", scannerPanel);
        addTab("Parser", parserPanel);
        addTab("Semantic", semanticPanel);
        addTab("Errors", errorsPanel);
        addTab("IR", irPanel);
        addTab("Run", interpreterPanel);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        scannerPanel.onCompileComplete(result);
        parserPanel.onCompileComplete(result);
        semanticPanel.onCompileComplete(result);
        errorsPanel.onCompileComplete(result);
        irPanel.onCompileComplete(result);
        interpreterPanel.onCompileComplete(result);
    }

    public ScannerOutputPanel getScannerPanel() { return scannerPanel; }
    public ParserOutputPanel getParserPanel() { return parserPanel; }
    public SemanticOutputPanel getSemanticPanel() { return semanticPanel; }
    public ErrorsPanel getErrorsPanel() { return errorsPanel; }
    public IrOutputPanel getIrPanel() { return irPanel; }
    public InterpreterOutputPanel getInterpreterPanel() { return interpreterPanel; }
}
