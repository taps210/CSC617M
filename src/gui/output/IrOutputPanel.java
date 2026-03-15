package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.Theme;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Header with IR stats + syntax-highlighted JTextPane showing three-address code.
 * Function headers, labels, control flow, and ABM instructions each get distinct colors.
 */
public class IrOutputPanel extends JPanel implements CompileListener {
    private static final String PLACEHOLDER = "(IR will appear here after a successful compilation with no errors.)";
    private final PanelHeader header;
    private final JTextPane textPane;

    public IrOutputPanel() {
        super(new BorderLayout());
        header = new PanelHeader();
        header.set("IR", Theme.PANEL_HEADER_NEUTRAL, "");

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

    private void renderIR(String text) {
        StyledDocument doc = textPane.getStyledDocument();
        try { doc.remove(0, doc.getLength()); } catch (BadLocationException ignored) {}

        SimpleAttributeSet aDefault  = attr(Theme.EDITOR_FG);
        SimpleAttributeSet aFunc     = attr(Theme.TOKEN_DECLARATION);
        SimpleAttributeSet aLabel    = attr(Theme.TOKEN_ABM_KEYWORD);
        SimpleAttributeSet aControl  = attr(Theme.TOKEN_KEYWORD);
        SimpleAttributeSet aCall     = attr(Theme.TOKEN_LITERAL_STRING);
        SimpleAttributeSet aAbm      = attr(Theme.TOKEN_ABM_KEYWORD);
        SimpleAttributeSet aType     = attr(Theme.TOKEN_TYPE);

        for (String line : text.split("\n", -1)) {
            String t = line.trim();
            SimpleAttributeSet style;
            if      (t.startsWith("function "))                        style = aFunc;
            else if (t.endsWith(":") && !t.contains(" "))              style = aLabel;
            else if (t.startsWith("goto ")   || t.startsWith("if ")
                  || t.equals("return")      || t.startsWith("return ")
                  || t.startsWith("param ")  || t.startsWith("print ")
                  || t.startsWith("read "))                            style = aControl;
            else if (t.contains("call "))                              style = aCall;
            else if (t.startsWith("spawn ")  || t.startsWith("move(")
                  || t.startsWith("step(")   || t.startsWith("destroy(")
                  || t.contains("neighbors(")|| t.startsWith("zone_enter ")) style = aAbm;
            else if (t.contains("new array"))                          style = aType;
            else                                                       style = aDefault;

            try { doc.insertString(doc.getLength(), line + "\n", style); }
            catch (BadLocationException ignored) {}
        }
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (result == null || result.irText().isEmpty()) {
            header.set("IR", Theme.PANEL_HEADER_NEUTRAL, "");
            showPlaceholder();
            textPane.setCaretPosition(0);
            return;
        }
        String stats = result.metrics().irInstrCount + " instructions  "
                + PanelHeader.ms(result.metrics().irTimeNs);
        header.set("IR", Theme.PANEL_HEADER_OK, stats);
        renderIR(result.irText().get());
        textPane.setCaretPosition(0);
    }
}
