package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;

import javax.swing.*;
import java.awt.*;

/**
 * Read-only panel showing the intermediate representation (three-address code) generated from the AST.
 * Only populated when compilation succeeds with no errors.
 */
public class IrOutputPanel extends JScrollPane implements CompileListener {
    private static final String PLACEHOLDER = "(IR output will appear here after a successful compilation with no errors.)";
    private final JTextArea textArea;

    public IrOutputPanel() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setMargin(new Insets(4, 4, 4, 4));
        setViewportView(textArea);
        textArea.setText(PLACEHOLDER);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (result == null) {
            textArea.setText(PLACEHOLDER);
            textArea.setCaretPosition(0);
            return;
        }
        String text = result.irText()
                .filter(s -> !s.isBlank())
                .orElse(PLACEHOLDER);
        textArea.setText(text);
        textArea.setCaretPosition(0);
    }
}
