package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;

import javax.swing.*;
import java.awt.*;

/**
 * v1: Read-only JTextArea showing parser trace. Parse tree is shown in the Analysis window.
 */
public class ParserOutputPanel extends JScrollPane implements CompileListener {
    private static final String PLACEHOLDER = "(Parser output will appear here after Run. Use valid Herd code to see trace.)";
    private final JTextArea textArea;

    public ParserOutputPanel() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setMargin(new Insets(4, 4, 4, 4));
        setViewportView(textArea);
        textArea.setText(PLACEHOLDER);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        String trace = result != null && result.parserTrace() != null ? result.parserTrace() : "";
        textArea.setText(trace.isEmpty() ? PLACEHOLDER : trace);
        textArea.setCaretPosition(0);
    }
}
