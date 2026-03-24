package src.gui.debug;

import src.gui.editor.EditorPanel;
import src.gui.editor.LineNumberComponent;
import src.ir.DebugFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Debugger panel displaying variables, call stack, breakpoints, and step controls.
 */
public class DebuggerPanel extends JPanel {
    private EditorPanel editorPanel;
    private LineNumberComponent lineNumberComponent;

    private JTable variablesTable;
    private JList<String> callStackList;
    private JList<String> breakpointsList;

    private JButton stepOverBtn;
    private JButton stepIntoBtn;
    private JButton stepOutBtn;
    private JButton continueBtn;
    private JButton stopBtn;
    private JButton removeBreakpointBtn;

    private DefaultTableModel variablesModel;
    private DefaultListModel<String> callStackModel;
    private DefaultListModel<String> breakpointsModel;

    // Callbacks for button actions
    private Runnable onStepOver = () -> {};
    private Runnable onStepInto = () -> {};
    private Runnable onStepOut = () -> {};
    private Runnable onContinue = () -> {};
    private Runnable onStop = () -> {};

    public DebuggerPanel() {
        super(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        // Header: "Debugger" label + status
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Debugger"), BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Main content: split panes for variables + call stack + breakpoints
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Variables and Call Stack panels (top)
        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Variables table
        variablesModel = new DefaultTableModel(new String[]{"Name", "Value"}, 0);
        variablesTable = new JTable(variablesModel);
        variablesTable.setEnabled(false);
        JScrollPane variablesScroll = new JScrollPane(variablesTable);
        topSplit.setLeftComponent(createLabeledPanel("Variables", variablesScroll));

        // Call stack list
        callStackModel = new DefaultListModel<>();
        callStackList = new JList<>(callStackModel);
        callStackList.setEnabled(false);
        JScrollPane callStackScroll = new JScrollPane(callStackList);
        topSplit.setRightComponent(createLabeledPanel("Call Stack", callStackScroll));
        topSplit.setDividerLocation(0.5);

        mainSplit.setTopComponent(topSplit);

        // Breakpoints panel (bottom)
        breakpointsModel = new DefaultListModel<>();
        breakpointsList = new JList<>(breakpointsModel);
        breakpointsList.setEnabled(false);
        JScrollPane breakpointsScroll = new JScrollPane(breakpointsList);

        JPanel breakpointsPanel = new JPanel(new BorderLayout());
        breakpointsPanel.add(new JLabel("Breakpoints"), BorderLayout.NORTH);
        JPanel breakpointsContent = new JPanel(new BorderLayout());
        breakpointsContent.add(breakpointsScroll, BorderLayout.CENTER);

        removeBreakpointBtn = new JButton("Remove");
        removeBreakpointBtn.setEnabled(false);
        removeBreakpointBtn.addActionListener(e -> {
            int selected = breakpointsList.getSelectedIndex();
            if (selected >= 0) breakpointsModel.remove(selected);
        });
        JPanel breakpointsBtnPanel = new JPanel();
        breakpointsBtnPanel.add(removeBreakpointBtn);
        breakpointsContent.add(breakpointsBtnPanel, BorderLayout.SOUTH);
        breakpointsPanel.add(breakpointsContent, BorderLayout.CENTER);

        mainSplit.setBottomComponent(breakpointsPanel);
        mainSplit.setDividerLocation(0.7);

        add(mainSplit, BorderLayout.CENTER);

        // Control buttons (bottom)
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        stepOverBtn = new JButton("Step Over");
        stepOverBtn.setEnabled(false);
        stepOverBtn.addActionListener(e -> onStepOver.run());
        controlPanel.add(stepOverBtn);

        stepIntoBtn = new JButton("Step Into");
        stepIntoBtn.setEnabled(false);
        stepIntoBtn.addActionListener(e -> onStepInto.run());
        controlPanel.add(stepIntoBtn);

        stepOutBtn = new JButton("Step Out");
        stepOutBtn.setEnabled(false);
        stepOutBtn.addActionListener(e -> onStepOut.run());
        controlPanel.add(stepOutBtn);

        continueBtn = new JButton("Continue");
        continueBtn.setEnabled(false);
        continueBtn.addActionListener(e -> onContinue.run());
        controlPanel.add(continueBtn);

        stopBtn = new JButton("Stop");
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> onStop.run());
        controlPanel.add(stopBtn);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createLabeledPanel(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    public void setEditorPanel(EditorPanel editorPanel) {
        this.editorPanel = editorPanel;
    }

    public void setLineNumberComponent(LineNumberComponent lineNumberComponent) {
        this.lineNumberComponent = lineNumberComponent;
    }

    public void setOnStepOverRequested(Runnable runnable) {
        this.onStepOver = runnable;
    }

    public void setOnStepIntoRequested(Runnable runnable) {
        this.onStepInto = runnable;
    }

    public void setOnStepOutRequested(Runnable runnable) {
        this.onStepOut = runnable;
    }

    public void setOnContinueRequested(Runnable runnable) {
        this.onContinue = runnable;
    }

    public void setOnStopRequested(Runnable runnable) {
        this.onStop = runnable;
    }

    public void onDebugPause(int line, Map<String, Object> store, List<DebugFrame> callStack) {
        SwingUtilities.invokeLater(() -> {
            // Update variables table
            variablesModel.setRowCount(0);
            for (Map.Entry<String, Object> entry : store.entrySet()) {
                variablesModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }

            // Update call stack list
            callStackModel.clear();
            for (DebugFrame frame : callStack) {
                String frameStr = frame.functionName() + ":" + frame.sourceLine();
                callStackModel.addElement(frameStr);
            }

            // Highlight debug line in editor
            if (editorPanel != null) {
                editorPanel.highlightDebugLine(line);
            }

            // Enable step buttons
            stepOverBtn.setEnabled(true);
            stepIntoBtn.setEnabled(true);
            stepOutBtn.setEnabled(true);
            continueBtn.setEnabled(true);
            stopBtn.setEnabled(true);

            // Switch to Debug tab
            Container parent = getParent();
            if (parent instanceof JTabbedPane tabPane) {
                tabPane.setSelectedComponent(this);
            }
        });
    }

    public void onDebugSessionEnded() {
        SwingUtilities.invokeLater(() -> {
            // Disable step buttons
            stepOverBtn.setEnabled(false);
            stepIntoBtn.setEnabled(false);
            stepOutBtn.setEnabled(false);
            continueBtn.setEnabled(false);
            stopBtn.setEnabled(false);

            // Clear debug highlight
            if (editorPanel != null) {
                editorPanel.clearDebugHighlight();
            }
        });
    }
}
