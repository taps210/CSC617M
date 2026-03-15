package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.CompileError;
import src.gui.model.Theme;
import src.gui.model.TokenColorMap;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Header badge (Parse: OK / FAILED) + JTable: Construct | Line | Col.
 * Rows are parsed from the parser trace emitted by Parser.emit().
 */
public class ParserOutputPanel extends JPanel implements CompileListener {
    private static final String[] COLUMNS = {"Construct", "Line", "Col"};
    private static final Pattern TRACE_LINE =
            Pattern.compile("^(.+) found in line (\\d+) column (\\d+)$");

    private final PanelHeader header;
    private final DefaultTableModel model;

    public ParserOutputPanel() {
        super(new BorderLayout());

        header = new PanelHeader();
        header.set("Parser", Theme.PANEL_HEADER_NEUTRAL, "");

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (result == null) {
            header.set("Parser", Theme.PANEL_HEADER_NEUTRAL, "");
            model.setRowCount(0);
            return;
        }
        boolean hasParseErrors = result.errors().stream()
                .anyMatch(e -> e.source() == CompileError.Source.PARSER);
        String badge = hasParseErrors ? "Parse: FAILED" : "Parse: OK";
        Color badgeColor = hasParseErrors ? Theme.PANEL_HEADER_FAIL : Theme.PANEL_HEADER_OK;
        header.set(badge, badgeColor, "");

        model.setRowCount(0);
        String trace = result.parserTrace() != null ? result.parserTrace() : "";
        for (String line : trace.split("\\r?\\n", -1)) {
            Matcher m = TRACE_LINE.matcher(line.trim());
            if (m.matches()) {
                model.addRow(new Object[]{m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))});
            }
        }
    }
}
