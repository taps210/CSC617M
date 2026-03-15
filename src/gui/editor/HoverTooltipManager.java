package src.gui.editor;

import src.gui.core.CompileController;
import src.gui.core.CompileResult;
import src.gui.model.Theme;
import src.semantic.SymbolEntry;
import src.semantic.SymbolTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Attaches hover tooltips to the editor's JTextPane using Swing's built-in ToolTipManager.
 * When the cursor rests over a function name, displays its signature and declaration line.
 * Colors are sourced from Theme to stay consistent with the IDE's visual style.
 */
public class HoverTooltipManager {
    private final JTextPane editor;
    private final CompileController controller;

    public HoverTooltipManager(JTextPane editor, CompileController controller) {
        this.editor = editor;
        this.controller = controller;

        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.registerComponent(editor);
        ttm.setInitialDelay(400);
        ttm.setDismissDelay(8000);
        ttm.setReshowDelay(200);

        editor.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                editor.setToolTipText(computeTooltip(e.getPoint()));
            }
        });
    }

    @SuppressWarnings("deprecation")
    private String computeTooltip(Point p) {
        CompileResult result = controller.getLastResult();
        if (result == null || result.symbolEntries().isEmpty()) return null;

        int offset = editor.viewToModel(p);
        if (offset < 0) return null;

        String text = editor.getText();
        if (offset >= text.length()) return null;

        // Find word boundaries around the cursor position
        int start = offset;
        while (start > 0 && isIdentChar(text.charAt(start - 1))) start--;
        int end = offset;
        while (end < text.length() && isIdentChar(text.charAt(end))) end++;
        if (start == end) return null;

        String word = text.substring(start, end);

        // Find a matching function entry in the symbol table
        SymbolEntry match = result.symbolEntries().stream()
                .filter(e -> e.kind() == SymbolTable.Kind.FUNCTION && e.name().equals(word))
                .findFirst()
                .orElse(null);

        return match != null ? buildTooltipHtml(match) : null;
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Builds an HTML tooltip string from a SymbolEntry using Theme colors.
     * typeStr format from SemanticAnalyzer: "returnType(param1Type, param2Type, ...)"
     */
    private static String buildTooltipHtml(SymbolEntry entry) {
        String typeStr  = entry.typeStr();
        int parenOpen   = typeStr.indexOf('(');
        String retType  = parenOpen >= 0 ? typeStr.substring(0, parenOpen).trim() : typeStr;
        String params   = (parenOpen >= 0 && typeStr.endsWith(")"))
                ? typeStr.substring(parenOpen + 1, typeStr.length() - 1).trim()
                : "";

        // Colors sourced from Theme constants
        String colFuncName  = hex(Theme.TOKEN_DECLARATION);  // yellowish — same as func/agent headers
        String colType      = hex(Theme.TOKEN_TYPE);          // teal — same as type keywords
        String colPunct     = hex(Theme.TOKEN_PUNCTUATION);   // grey  — parens, arrow
        String colComment   = hex(Theme.TOKEN_COMMENT);       // green — "defined at line N"

        StringBuilder sb = new StringBuilder("<html><body>");

        // Signature: funcName(param1, param2) → returnType
        sb.append("<font color='").append(colFuncName).append("'>").append(entry.name()).append("</font>");
        sb.append("<font color='").append(colPunct).append("'>(</font>");
        if (!params.isEmpty()) {
            String[] parts = params.split(",\\s*");
            for (int i = 0; i < parts.length; i++) {
                sb.append("<font color='").append(colType).append("'>").append(parts[i].trim()).append("</font>");
                if (i < parts.length - 1)
                    sb.append("<font color='").append(colPunct).append("'>, </font>");
            }
        }
        sb.append("<font color='").append(colPunct).append("'>)</font>");
        sb.append("&nbsp;<font color='").append(colPunct).append("'>\u2192</font>&nbsp;");
        sb.append("<font color='").append(colType).append("'>").append(retType).append("</font>");

        // Declaration hint
        sb.append("<br><font color='").append(colComment).append("'><small>defined at line ")
          .append(entry.line()).append("</small></font>");

        sb.append("</body></html>");
        return sb.toString();
    }

    /** Converts a Color to an HTML hex string (#RRGGBB). */
    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }
}
