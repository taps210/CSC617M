package src.gui.debug;

import src.gui.editor.EditorPanel;
import src.gui.editor.LineNumberComponent;
import src.ir.DebugFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debugger panel displaying variables, watch expressions, trace log,
 * call stack, breakpoints, and step controls.
 */
public class DebuggerPanel extends JPanel {
    private EditorPanel editorPanel;
    private LineNumberComponent lineNumberComponent;

    private JTable variablesTable;
    private JList<String> callStackList;
    private JList<String> breakpointsList;

    private JButton nextLineBtn;
    private JButton continueBtn;
    private JButton restartBtn;
    private JButton removeBreakpointBtn;

    private DefaultTableModel variablesModel;
    private DefaultListModel<String> callStackModel;
    private DefaultListModel<String> breakpointsModel;

    // Watch panel
    private JComboBox<String> watchComboBox;
    private JButton addWatchBtn;
    private JButton removeWatchBtn;
    private DefaultTableModel watchModel;
    private JTable watchTable;
    private final List<String> watchExpressions = new ArrayList<>();

    // Trace panel
    private DefaultTableModel traceModel;
    private JTable traceTable;
    private JButton clearTraceBtn;
    private int traceStep = 0;
    private Map<String, Object> previousStore; // for detecting changes

    // Last known store and heap for watch evaluation
    private Map<String, Object> lastStore;
    private Map<Integer, Map<String, Object>> lastHeap;

    // Callbacks for button actions
    private Runnable onStepOver = () -> {};
    private Runnable onContinue = () -> {};
    private Runnable onStop = () -> {};

    public DebuggerPanel() {
        super(new BorderLayout());
        initComponents();
    }

    private void initComponents() {
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Debugger"), BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Use a tabbed pane for the main content area
        JTabbedPane contentTabs = new JTabbedPane(JTabbedPane.TOP);

        // === Tab 1: Variables + Call Stack ===
        JSplitPane varsCallSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        variablesModel = new DefaultTableModel(new String[]{"Name", "Value"}, 0);
        variablesTable = new JTable(variablesModel);
        variablesTable.setEnabled(false);
        varsCallSplit.setLeftComponent(createLabeledPanel("Variables", new JScrollPane(variablesTable)));

        callStackModel = new DefaultListModel<>();
        callStackList = new JList<>(callStackModel);
        callStackList.setEnabled(false);
        varsCallSplit.setRightComponent(createLabeledPanel("Call Stack", new JScrollPane(callStackList)));
        varsCallSplit.setDividerLocation(250);

        contentTabs.addTab("Variables", varsCallSplit);

        // === Tab 2: Watch ===
        JPanel watchPanel = new JPanel(new BorderLayout());

        // Watch input row
        JPanel watchInputPanel = new JPanel(new BorderLayout(4, 0));
        watchInputPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        watchComboBox = new JComboBox<>();
        watchComboBox.setEditable(false);
        watchComboBox.setToolTipText("Select a variable to watch");
        addWatchBtn = new JButton("Add");
        addWatchBtn.addActionListener(e -> addWatch());
        removeWatchBtn = new JButton("Remove");
        removeWatchBtn.addActionListener(e -> removeSelectedWatch());

        JPanel watchBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        watchBtnPanel.add(addWatchBtn);
        watchBtnPanel.add(removeWatchBtn);
        watchInputPanel.add(new JLabel("Watch: "), BorderLayout.WEST);
        watchInputPanel.add(watchComboBox, BorderLayout.CENTER);
        watchInputPanel.add(watchBtnPanel, BorderLayout.EAST);
        watchPanel.add(watchInputPanel, BorderLayout.NORTH);

        // Watch table
        watchModel = new DefaultTableModel(new String[]{"Expression", "Value"}, 0);
        watchTable = new JTable(watchModel);
        watchTable.setEnabled(true);
        watchPanel.add(new JScrollPane(watchTable), BorderLayout.CENTER);

        contentTabs.addTab("Watch", watchPanel);

        // === Tab 3: Trace ===
        JPanel tracePanel = new JPanel(new BorderLayout());

        // Trace header with clear button
        JPanel traceHeaderPanel = new JPanel(new BorderLayout());
        traceHeaderPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        traceHeaderPanel.add(new JLabel("Execution Trace (records variable values at each step)"), BorderLayout.CENTER);
        clearTraceBtn = new JButton("Clear");
        clearTraceBtn.addActionListener(e -> clearTrace());
        traceHeaderPanel.add(clearTraceBtn, BorderLayout.EAST);
        tracePanel.add(traceHeaderPanel, BorderLayout.NORTH);

        // Trace table — one row per step, compact summary
        traceModel = new DefaultTableModel(new String[]{"Step", "Line", "Changes"}, 0);
        traceTable = new JTable(traceModel);
        traceTable.setEnabled(false);
        traceTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tracePanel.add(new JScrollPane(traceTable), BorderLayout.CENTER);

        contentTabs.addTab("Trace", tracePanel);

        // === Tab 4: Breakpoints ===
        breakpointsModel = new DefaultListModel<>();
        breakpointsList = new JList<>(breakpointsModel);
        breakpointsList.setEnabled(false);

        JPanel breakpointsPanel = new JPanel(new BorderLayout());
        breakpointsPanel.add(new JScrollPane(breakpointsList), BorderLayout.CENTER);

        removeBreakpointBtn = new JButton("Remove");
        removeBreakpointBtn.setEnabled(false);
        removeBreakpointBtn.addActionListener(e -> {
            int selected = breakpointsList.getSelectedIndex();
            if (selected >= 0) breakpointsModel.remove(selected);
        });
        JPanel breakpointsBtnPanel = new JPanel();
        breakpointsBtnPanel.add(removeBreakpointBtn);
        breakpointsPanel.add(breakpointsBtnPanel, BorderLayout.SOUTH);

        contentTabs.addTab("Breakpoints", breakpointsPanel);

        add(contentTabs, BorderLayout.CENTER);

        // Control buttons (bottom) — simplified: Next Line, Continue, Restart
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        nextLineBtn = new JButton("Next Line");
        nextLineBtn.setEnabled(false);
        nextLineBtn.addActionListener(e -> onStepOver.run());
        controlPanel.add(nextLineBtn);

        continueBtn = new JButton("Continue");
        continueBtn.setEnabled(false);
        continueBtn.addActionListener(e -> onContinue.run());
        controlPanel.add(continueBtn);

        restartBtn = new JButton("Restart");
        restartBtn.setEnabled(false);
        restartBtn.addActionListener(e -> {
            onStop.run();
            // Clear trace for fresh run
            clearTrace();
        });
        controlPanel.add(restartBtn);

        add(controlPanel, BorderLayout.SOUTH);
    }

    // --- Watch helpers ---

    private void addWatch() {
        Object selected = watchComboBox.getSelectedItem();
        if (selected == null) return;
        String expr = selected.toString().trim();
        if (expr.isEmpty()) return;
        if (watchExpressions.contains(expr)) return;
        watchExpressions.add(expr);
        refreshWatch();
    }

    private void removeSelectedWatch() {
        int row = watchTable.getSelectedRow();
        if (row >= 0 && row < watchExpressions.size()) {
            watchExpressions.remove(row);
            refreshWatch();
        }
    }

    private void refreshWatch() {
        watchModel.setRowCount(0);
        for (String expr : watchExpressions) {
            String value = evaluateWatch(expr);
            watchModel.addRow(new Object[]{expr, value});
            // Expand pointer fields as sub-rows
            if (lastStore != null) {
                Object val = lastStore.get(expr);
                if (isHeapPointer(val)) {
                    Map<String, Object> fields = getHeapFields(val);
                    if (fields != null) {
                        for (Map.Entry<String, Object> f : fields.entrySet()) {
                            if ("_type".equals(f.getKey())) continue;
                            watchModel.addRow(new Object[]{"  " + expr + "->" + f.getKey(), formatValue(f.getValue())});
                        }
                    }
                }
            }
        }
    }

    private void updateWatchComboBox(Map<String, Object> store) {
        watchComboBox.removeAllItems();
        for (String key : store.keySet()) {
            if (key.matches("t\\d+")) continue; // skip IR temps
            if (!watchExpressions.contains(key)) {
                watchComboBox.addItem(key);
            }
        }
    }

    private String evaluateWatch(String expr) {
        if (lastStore == null) return "<no debug session>";
        Object val = lastStore.get(expr);
        if (val == null && !lastStore.containsKey(expr)) return "<undefined>";
        return formatValue(val);
    }

    // --- Trace helpers ---

    private void recordTrace(int line, Map<String, Object> store) {
        traceStep++;

        // Only show variables that actually changed since the last step
        StringBuilder changes = new StringBuilder();

        for (Map.Entry<String, Object> entry : store.entrySet()) {
            String key = entry.getKey();
            if (key.matches("t\\d+")) continue;
            if (!watchExpressions.isEmpty() && !watchExpressions.contains(key)) continue;

            Object val = entry.getValue();
            Object prev = previousStore != null ? previousStore.get(key) : null;

            // Emit only if new or changed
            boolean changed = previousStore == null
                    || !previousStore.containsKey(key)
                    || !String.valueOf(val).equals(String.valueOf(prev));
            if (!changed) continue;

            if (changes.length() > 0) changes.append(", ");
            changes.append(key).append("=").append(formatValue(val));

            // Expand pointer fields inline
            if (isHeapPointer(val) && lastHeap != null) {
                Map<String, Object> fields = getHeapFields(val);
                if (fields != null) {
                    changes.append("{");
                    boolean first = true;
                    for (Map.Entry<String, Object> f : fields.entrySet()) {
                        if ("_type".equals(f.getKey())) continue;
                        if (!first) changes.append(", ");
                        changes.append(f.getKey()).append("=").append(formatValue(f.getValue()));
                        first = false;
                    }
                    changes.append("}");
                }
            }
        }

        previousStore = new HashMap<>(store);
        traceModel.addRow(new Object[]{traceStep, line, changes.length() > 0 ? changes.toString() : "(no change)"});

        // Auto-scroll to bottom
        SwingUtilities.invokeLater(() -> {
            int lastRow = traceTable.getRowCount() - 1;
            if (lastRow >= 0) traceTable.scrollRectToVisible(traceTable.getCellRect(lastRow, 0, true));
        });
    }

    private void clearTrace() {
        traceModel.setRowCount(0);
        traceStep = 0;
        previousStore = null;
    }

    // --- Shared helpers ---

    private Map<String, Object> getHeapFields(Object ptr) {
        if (lastHeap == null || ptr == null) return null;
        try {
            String idStr = ptr.toString().substring(1);
            int objectId = Integer.parseInt(idStr);
            return lastHeap.get(objectId);
        } catch (Exception e) {
            return null;
        }
    }

    private void expandHeapPointer(String varName, Object ptr, Map<Integer, Map<String, Object>> heapSnapshot) {
        if (heapSnapshot == null) return;
        try {
            // Extract objectId from HeapPointer via toString() which returns "#N"
            String idStr = ptr.toString().substring(1); // remove "#"
            int objectId = Integer.parseInt(idStr);
            Map<String, Object> fields = heapSnapshot.get(objectId);
            if (fields == null) return;
            for (Map.Entry<String, Object> field : fields.entrySet()) {
                if ("_type".equals(field.getKey())) continue; // skip internal type marker
                variablesModel.addRow(new Object[]{"  " + varName + "->" + field.getKey(), formatValue(field.getValue())});
            }
        } catch (Exception ignored) {
            // Skip if we can't parse the pointer
        }
    }

    private static boolean isHeapPointer(Object val) {
        if (val == null) return false;
        String s = val.toString();
        return s.startsWith("#") && s.length() > 1 && Character.isDigit(s.charAt(1));
    }

    private static String formatValue(Object val) {
        if (val == null) return "null";
        if (isHeapPointer(val)) return val.toString();
        if (val instanceof List<?>) return "array[" + ((List<?>) val).size() + "]";
        return String.valueOf(val);
    }

    private JPanel createLabeledPanel(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    // --- Public API ---

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
        // Maps to Next Line (same as step over for simplified UI)
        this.onStepOver = runnable;
    }

    public void setOnStepOutRequested(Runnable runnable) {
        // Not exposed in simplified UI
    }

    public void setOnContinueRequested(Runnable runnable) {
        this.onContinue = runnable;
    }

    public void setOnStopRequested(Runnable runnable) {
        this.onStop = runnable;
    }

    public void onDebugPause(int line, Map<String, Object> store, List<DebugFrame> callStack,
                             Map<Integer, Map<String, Object>> heapSnapshot) {
        SwingUtilities.invokeLater(() -> {
            // Save store and heap for watch evaluation
            lastStore = store;
            lastHeap = heapSnapshot;

            // Update variables table (filter out IR temp variables)
            variablesModel.setRowCount(0);
            for (Map.Entry<String, Object> entry : store.entrySet()) {
                if (entry.getKey().matches("t\\d+")) continue; // skip IR temps
                Object val = entry.getValue();
                variablesModel.addRow(new Object[]{entry.getKey(), formatValue(val)});
                // If the value is a HeapPointer, expand its fields as sub-rows
                if (val != null && isHeapPointer(val)) {
                    expandHeapPointer(entry.getKey(), val, heapSnapshot);
                }
            }

            // Update watch panel
            updateWatchComboBox(store);
            refreshWatch();

            // Record trace entry
            recordTrace(line, store);

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

            // Enable buttons
            nextLineBtn.setEnabled(true);
            continueBtn.setEnabled(true);
            restartBtn.setEnabled(true);

            // Switch to Debug tab
            Container parent = getParent();
            if (parent instanceof JTabbedPane tabPane) {
                tabPane.setSelectedComponent(this);
            }
        });
    }

    public void onDebugSessionEnded() {
        SwingUtilities.invokeLater(() -> {
            lastStore = null;

            // Disable buttons
            nextLineBtn.setEnabled(false);
            continueBtn.setEnabled(false);
            restartBtn.setEnabled(false);

            // Clear debug highlight
            if (editorPanel != null) {
                editorPanel.clearDebugHighlight();
            }
        });
    }
}
