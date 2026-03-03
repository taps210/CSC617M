package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;

import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Status bar: compile status, caret position (Line X, Col Y), error count.
 */
public class StatusBar extends JPanel implements CompileListener {
    private final JLabel statusLabel;
    private final JLabel positionLabel;
    private final JLabel errorsLabel;

    public StatusBar() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setBorder(BorderFactory.createEtchedBorder());
        statusLabel = new JLabel("Ready");
        positionLabel = new JLabel("Line 1, Col 1");
        errorsLabel = new JLabel("0 errors");
        add(statusLabel);
        add(Box.createHorizontalStrut(16));
        add(positionLabel);
        add(Box.createHorizontalGlue());
        add(errorsLabel);
    }

    public void attachToEditor(JTextComponent editor) {
        editor.addCaretListener(e -> updatePosition(editor, e.getDot()));
    }

    private void updatePosition(JTextComponent editor, int dot) {
        try {
            javax.swing.text.Document doc = editor.getDocument();
            int line = 1;
            int col = 1;
            if (doc.getLength() > 0 && dot <= doc.getLength()) {
                int off = Math.min(dot, doc.getLength() - 1);
                Element root = doc.getDefaultRootElement();
                int lineIndex = root.getElementIndex(off);
                line = lineIndex + 1;
                Element lineEl = root.getElement(lineIndex);
                col = off - lineEl.getStartOffset() + 1;
            }
            positionLabel.setText("Line " + line + ", Col " + col);
        } catch (Exception ignored) {
            positionLabel.setText("Line 1, Col 1");
        }
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        setStatus("Ready");
        errorsLabel.setText(result.errors().size() + " error(s)");
    }
}
