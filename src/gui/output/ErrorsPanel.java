package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.CompileError;
import src.gui.model.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Header badge (N errors) + JTable: # | Source | Line | Col | Message.
 * Row click moves editor caret to that line/col. ERROR rows in red, WARN in orange.
 */
public class ErrorsPanel extends JPanel implements CompileListener {
    private static final String[] COLUMNS = {"#", "Source", "Line", "Col", "Message"};
    private final PanelHeader header;
    private final JTable table;
    private final DefaultTableModel model;
    private List<CompileError> errors = java.util.Collections.emptyList();
    private Runnable onErrorSelected;

    public ErrorsPanel() {
        super(new BorderLayout());
        header = new PanelHeader();
        header.set("Errors", Theme.PANEL_HEADER_NEUTRAL, "");

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                if (row >= 0 && row < errors.size()) {
                    c.setForeground(errors.get(row).severity() == CompileError.Severity.WARN
                            ? Color.ORANGE.darker() : Color.RED);
                }
                return c;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || onErrorSelected == null) return;
            int row = table.getSelectedRow();
            if (row >= 0 && row < errors.size()) onErrorSelected.run();
        });
        table.getTableHeader().setReorderingAllowed(false);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        javax.swing.table.TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(40);   // #
        cm.getColumn(1).setPreferredWidth(80);   // Source
        cm.getColumn(2).setPreferredWidth(50);   // Line
        cm.getColumn(3).setPreferredWidth(50);   // Col
        cm.getColumn(4).setPreferredWidth(600);  // Message

        JScrollPane scroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void setOnErrorSelected(Runnable onErrorSelected) {
        this.onErrorSelected = onErrorSelected;
    }

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
            model.addRow(new Object[]{n++, e.source().name(), e.line(), e.col(), e.message()});
        }
        int errorCount = (int) errors.stream()
                .filter(e -> e.severity() == CompileError.Severity.ERROR).count();
        int warnCount  = (int) errors.stream()
                .filter(e -> e.severity() == CompileError.Severity.WARN).count();
        if (errorCount == 0 && warnCount == 0) {
            header.set("No errors", Theme.PANEL_HEADER_OK, "");
        } else {
            String badge = errorCount + " error(s)" + (warnCount > 0 ? "  " + warnCount + " warning(s)" : "");
            header.set(badge, Theme.PANEL_HEADER_FAIL, "");
        }
    }
}
