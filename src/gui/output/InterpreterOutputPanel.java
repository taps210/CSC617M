package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;

import javax.swing.*;
import java.awt.*;

/**
 * Read-only panel showing the stdout produced by running the interpreter on the compiled IR.
 * Only populated when compilation succeeds with no errors.
 * Note: programs that use read() will show a runtime error since the IDE does not supply stdin.
 */
public class InterpreterOutputPanel extends JScrollPane implements CompileListener {
    private static final String PLACEHOLDER = "(Run output will appear here after a successful compilation with no errors.)";
    private final JTextArea textArea;

    public InterpreterOutputPanel() {
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
        String text = result.interpreterOutput()
                .map(s -> s.isBlank() ? "(Program produced no output.)" : s)
                .orElse(PLACEHOLDER);
        textArea.setText(text);
        textArea.setCaretPosition(0);
    }
}
