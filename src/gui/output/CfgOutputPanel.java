package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.Theme;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Header with CFG stats + syntax-highlighted JTextPane.
 * Block headers, successor arrows, and labels each get distinct colors.
 */
public class CfgOutputPanel extends JPanel implements CompileListener {
    private static final String PLACEHOLDER = "(CFG will appear here after a successful compilation with no errors.)";
    private final PanelHeader header;
    private final JTextPane textPane;

    public CfgOutputPanel() {
        super(new BorderLayout());
        header = new PanelHeader();
        header.set("CFG", Theme.PANEL_HEADER_NEUTRAL, "");

        textPane = new JTextPane();
        textPane.setEditable(false);

        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        showPlaceholder();
    }

    private SimpleAttributeSet attr(Color fg) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, Font.MONOSPACED);
        StyleConstants.setFontSize(a, 14);
        StyleConstants.setForeground(a, fg);
        return a;
    }

    private void showPlaceholder() {
        textPane.setText(PLACEHOLDER);
        textPane.getStyledDocument().setCharacterAttributes(
                0, PLACEHOLDER.length(), attr(Theme.LINE_NUMBER_FG), true);
    }

    private void renderCFG(String text) {
        StyledDocument doc = textPane.getStyledDocument();
        try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}

        SimpleAttributeSet aDefault = attr(Theme.EDITOR_FG);
        SimpleAttributeSet aBlock   = attr(Theme.TOKEN_DECLARATION);
        SimpleAttributeSet aSucc    = attr(Theme.TOKEN_KEYWORD);
        SimpleAttributeSet aLabel   = attr(Theme.TOKEN_ABM_KEYWORD);

        for (String line : text.split("\n", -1)) {
            String t = line.trim();
            SimpleAttributeSet style;
            if      (t.startsWith("block "))                      style = aBlock;
            else if (t.startsWith("-> "))                         style = aSucc;
            else if (t.endsWith(":") && !t.contains(" "))         style = aLabel;
            else                                                  style = aDefault;

            try { doc.insertString(doc.getLength(), line + "\n", style); }
            catch (BadLocationException ignored) {}
        }
    }

    private int countBlocks(String cfgText) {
        int count = 0;
        for (String line : cfgText.split("\n", -1)) {
            if (line.trim().startsWith("block ")) count++;
        }
        return count;
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (result == null || result.cfgText().isEmpty()) {
            header.set("CFG", Theme.PANEL_HEADER_NEUTRAL, "");
            showPlaceholder();
            textPane.setCaretPosition(0);
            return;
        }
        String cfgText = result.cfgText().get();
        int blocks = countBlocks(cfgText);
        header.set("CFG", Theme.PANEL_HEADER_OK, blocks + " blocks");
        renderCFG(cfgText);
        textPane.setCaretPosition(0);
    }
}
