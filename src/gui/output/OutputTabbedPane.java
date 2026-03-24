package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.debug.DebuggerPanel;
import src.gui.output.OptimizationOutputPanel;

import javax.swing.*;
import java.awt.Dimension;

/**
 * Holds all output tabs. Delegates CompileListener to children.
 */
public class OutputTabbedPane extends JTabbedPane implements CompileListener {
    private final ScannerOutputPanel scannerPanel;
    private final ParserOutputPanel parserPanel;
    private final AnalyzePanel analyzePanel;
    private final SemanticOutputPanel semanticPanel;
    private final IrOutputPanel irPanel;
    private final CfgOutputPanel cfgPanel;
    private final OptimizationOutputPanel optimizationPanel;
    private final ErrorsPanel errorsPanel;
    private final InterpreterOutputPanel interpreterPanel;
    private final DebuggerPanel debuggerPanel;

    private static final int MIN_PANEL_WIDTH = 320;
    private static final int MIN_PANEL_HEIGHT = 200;

    public OutputTabbedPane() {
        scannerPanel       = new ScannerOutputPanel();
        parserPanel        = new ParserOutputPanel();
        analyzePanel       = new AnalyzePanel();
        semanticPanel      = new SemanticOutputPanel();
        irPanel            = new IrOutputPanel();
        cfgPanel           = new CfgOutputPanel();
        optimizationPanel  = new OptimizationOutputPanel();
        errorsPanel        = new ErrorsPanel();
        interpreterPanel   = new InterpreterOutputPanel();
        debuggerPanel      = new DebuggerPanel();

        for (JComponent p : new JComponent[]{
                scannerPanel, parserPanel, analyzePanel, semanticPanel,
                irPanel, cfgPanel, optimizationPanel, errorsPanel, interpreterPanel, debuggerPanel}) {
            p.setMinimumSize(new Dimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT));
        }

        addTab("Scanner",      scannerPanel);
        addTab("Parser",       parserPanel);
        addTab("Trees",        analyzePanel);
        addTab("Semantic",     semanticPanel);
        addTab("Optimization", optimizationPanel);
        addTab("IR",           irPanel);
        addTab("CFG",          cfgPanel);
        addTab("Errors",       errorsPanel);
        addTab("Output",       interpreterPanel);
        addTab("Debug",        debuggerPanel);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        scannerPanel.onCompileComplete(result);
        parserPanel.onCompileComplete(result);
        analyzePanel.onCompileComplete(result);
        semanticPanel.onCompileComplete(result);
        irPanel.onCompileComplete(result);
        cfgPanel.onCompileComplete(result);
        optimizationPanel.onCompileComplete(result);
        errorsPanel.onCompileComplete(result);
        interpreterPanel.onCompileComplete(result);
    }

    public ScannerOutputPanel getScannerPanel()         { return scannerPanel; }
    public ParserOutputPanel getParserPanel()           { return parserPanel; }
    public AnalyzePanel getAnalyzePanel()               { return analyzePanel; }
    public SemanticOutputPanel getSemanticPanel()       { return semanticPanel; }
    public IrOutputPanel getIrPanel()                   { return irPanel; }
    public CfgOutputPanel getCfgPanel()                 { return cfgPanel; }
    public ErrorsPanel getErrorsPanel()                 { return errorsPanel; }
    public InterpreterOutputPanel getInterpreterPanel() { return interpreterPanel; }
    public DebuggerPanel getDebuggerPanel()             { return debuggerPanel; }
}
