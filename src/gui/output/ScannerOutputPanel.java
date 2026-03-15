package src.gui.output;

import src.Token;
import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.CompileError;
import src.gui.model.Theme;
import src.gui.model.TokenColorMap;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Header badge (Scan: OK / N errors) + JTable: Type | Lexeme | Line | Col. Rows colored by TokenColorMap.
 */
public class ScannerOutputPanel extends JPanel implements CompileListener {
    private static final String[] COLUMNS = {"Type", "Lexeme", "Line", "Col"};
    private final PanelHeader header;
    private final JTable table;
    private final DefaultTableModel model;
    private List<Token> tokenList = java.util.Collections.emptyList();

    public ScannerOutputPanel() {
        super(new BorderLayout());

        header = new PanelHeader();
        header.set("Scanner", Theme.PANEL_HEADER_NEUTRAL, "");

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                Token token = getTokenAtRow(row);
                if (token != null) {
                    c.setForeground(TokenColorMap.getColor(token));
                }
                return c;
            }
        });
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private Token getTokenAtRow(int row) {
        if (row < 0 || row >= tokenList.size()) return null;
        return tokenList.get(row);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        tokenList = result.tokens();
        model.setRowCount(0);
        for (Token t : tokenList) {
            model.addRow(new Object[]{t.type().name(), t.lexeme(), t.line(), t.col()});
        }
        boolean hasLexErrors = result.errors().stream()
                .anyMatch(e -> e.source() == CompileError.Source.LEXER);
        String badge = hasLexErrors ? "Scan: FAILED" : "Scan: OK";
        Color badgeColor = hasLexErrors ? Theme.PANEL_HEADER_FAIL : Theme.PANEL_HEADER_OK;
        header.set(badge, badgeColor, "");
    }
}
