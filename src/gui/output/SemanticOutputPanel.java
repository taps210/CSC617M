package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.CompileError;
import src.gui.model.Theme;
import src.semantic.SymbolEntry;
import src.semantic.SymbolTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Two-section panel: Symbol Table (top) + Analysis output (bottom).
 * Sections are separated by a JSplitPane; each has a SectionDivider title.
 */
public class SemanticOutputPanel extends JPanel implements CompileListener {
    private static final String[] SYMBOL_COLUMNS = {"Scope", "Name", "Kind", "Type", "Line"};

    private final PanelHeader header;
    private final DefaultTableModel symbolModel;
    private final JTextArea analysisArea;

    public SemanticOutputPanel() {
        super(new BorderLayout());

        header = new PanelHeader();
        header.set("Semantic", Theme.PANEL_HEADER_NEUTRAL, "");

        // --- Symbol Table section ---
        symbolModel = new DefaultTableModel(SYMBOL_COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable symbolTable = new JTable(symbolModel);
        symbolTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        symbolTable.getTableHeader().setReorderingAllowed(false);
        symbolTable.setShowGrid(false);
        symbolTable.setIntercellSpacing(new Dimension(0, 0));

        javax.swing.table.TableColumnModel cm = symbolTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(100);  // Scope
        cm.getColumn(1).setPreferredWidth(120);  // Name
        cm.getColumn(2).setPreferredWidth(80);   // Kind
        cm.getColumn(3).setPreferredWidth(120);  // Type
        cm.getColumn(4).setPreferredWidth(50);   // Line
        symbolTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane symbolScroll = new JScrollPane(symbolTable);
        symbolScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel symbolSection = new JPanel(new BorderLayout());
        symbolSection.add(new SectionDivider("Symbol Table"), BorderLayout.NORTH);
        symbolSection.add(symbolScroll, BorderLayout.CENTER);

        // --- Analysis section ---
        analysisArea = new JTextArea("(Semantic analysis will appear here after Run.)");
        analysisArea.setEditable(false);
        analysisArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        analysisArea.setMargin(new Insets(4, 6, 4, 4));

        JScrollPane analysisScroll = new JScrollPane(analysisArea);
        analysisScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel analysisSection = new JPanel(new BorderLayout());
        analysisSection.add(new SectionDivider("Analysis"), BorderLayout.NORTH);
        analysisSection.add(analysisScroll, BorderLayout.CENTER);

        // --- Split ---
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, symbolSection, analysisSection);
        split.setResizeWeight(0.65);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(4);

        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (result == null) {
            header.set("Semantic", Theme.PANEL_HEADER_NEUTRAL, "");
            symbolModel.setRowCount(0);
            analysisArea.setText("(Semantic analysis will appear here after Run.)");
            return;
        }

        // Populate symbol table
        List<SymbolEntry> symbols = result.symbolEntries();
        symbolModel.setRowCount(0);
        for (SymbolEntry s : symbols) {
            symbolModel.addRow(new Object[]{s.scope(), s.name(), kindLabel(s.kind()), s.typeStr(), s.line()});
        }

        // Populate analysis text
        List<CompileError> semanticErrors = result.errors().stream()
                .filter(e -> e.source() == CompileError.Source.SEMANTIC)
                .toList();
        int count = semanticErrors.size();
        boolean astPresent = result.ast().isPresent();

        String badge;
        Color badgeColor;
        StringBuilder sb = new StringBuilder();

        if (!astPresent) {
            badge = "Not run";
            badgeColor = Theme.PANEL_HEADER_NEUTRAL;
            sb.append("Semantic analysis not run — parse failed or produced no AST.");
        } else if (count > 0) {
            badge = count + " error(s)";
            badgeColor = Theme.PANEL_HEADER_FAIL;
            for (CompileError e : semanticErrors) {
                sb.append("L").append(e.line()).append(": ").append(e.message())
                  .append(System.lineSeparator());
            }
        } else {
            badge = "Semantic: OK";
            badgeColor = Theme.PANEL_HEADER_OK;
            sb.append("No semantic errors found.");
        }
        String stats = symbols.isEmpty() ? "" : symbols.size() + " symbols";
        header.set(badge, badgeColor, stats);
        analysisArea.setText(sb.toString());
        analysisArea.setCaretPosition(0);
    }

    private static String kindLabel(SymbolTable.Kind kind) {
        return switch (kind) {
            case VARIABLE -> "var";
            case CONSTANT -> "const";
            case TYPE     -> "type";
            case FUNCTION -> "func";
            case AGENT    -> "agent";
            case WORLD    -> "world";
        };
    }
}
