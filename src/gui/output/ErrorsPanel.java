package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.CompileError;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * JTable: # | Line | Col | Message. Row click moves editor caret to that line/col.
 * ERROR rows in red, WARN in orange.
 */
public class ErrorsPanel extends JScrollPane implements CompileListener {
    private static final String[] COLUMNS = {"#", "Line", "Col", "Message"};
    private final JTable table;
    private final DefaultTableModel model;
    private List<CompileError> errors = java.util.Collections.emptyList();
    private Runnable onErrorSelected; // (line, col) -> move caret

    public ErrorsPanel() {
        model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (row >= 0 && row < errors.size()) {
                    c.setForeground(errors.get(row).severity() == CompileError.Severity.WARN ? Color.ORANGE.darker() : Color.RED);
                }
                return c;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || onErrorSelected == null) return;
            int row = table.getSelectedRow();
            if (row >= 0 && row < errors.size()) onErrorSelected.run();
        });
        javax.swing.table.TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(28);   // #
        colModel.getColumn(1).setPreferredWidth(44);   // Line
        colModel.getColumn(2).setPreferredWidth(44);   // Col
        colModel.getColumn(3).setPreferredWidth(500); // Message - wide so full text is visible
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setViewportView(table);
    }

    public void setOnErrorSelected(Runnable onErrorSelected) {
        this.onErrorSelected = onErrorSelected;
    }

    /** Call this from the callback to get the currently selected error and move editor caret. */
    public CompileError getSelectedError() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < errors.size()) return errors.get(row);
        return null;
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        errors = result.errors();
        model.setRowCount(0);
        int n = 1;
        for (CompileError e : errors) {
            model.addRow(new Object[]{n++, e.line(), e.col(), e.message()});
        }
    }
}
