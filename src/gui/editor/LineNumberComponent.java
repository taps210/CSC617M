package src.gui.editor;

import src.gui.model.Theme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import java.awt.*;

/**
 * Paints line numbers in the row header of the editor scroll pane.
 */
public class LineNumberComponent extends JComponent {
    private final JTextPane editor;
    private static final int MARGIN = 4;
    private static final int MIN_WIDTH = 36;

    public LineNumberComponent(JTextPane editor) {
        this.editor = editor;
        setFont(editor.getFont());
        setBackground(Theme.LINE_NUMBER_BG);
        setForeground(Theme.LINE_NUMBER_FG);
        updatePreferredWidth();
        setMinimumSize(new Dimension(MIN_WIDTH, 0));
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updatePreferredWidth(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updatePreferredWidth(); }
            @Override
            public void changedUpdate(DocumentEvent e) { }
        });
    }

    private void updatePreferredWidth() {
        FontMetrics fm = getFontMetrics(getFont());
        if (fm == null) {
            setPreferredSize(new Dimension(MIN_WIDTH, Short.MAX_VALUE));
            return;
        }
        int lineCount = editor.getDocument().getDefaultRootElement().getElementCount();
        int digits = Math.max(2, lineCount <= 1 ? 1 : (int) Math.log10(lineCount) + 1);
        int w = Math.max(MIN_WIDTH, MARGIN * 2 + fm.charWidth('0') * digits);
        setPreferredSize(new Dimension(w, Short.MAX_VALUE));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Rectangle clip = g.getClipBounds();
        g2.setColor(getBackground());
        g2.fill(clip);
        g2.setColor(getForeground());
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();
        int baseLine = fm.getAscent();
        try {
            Element root = editor.getDocument().getDefaultRootElement();
            int startLine = Math.max(1, clip.y / lineHeight);
            int endLine = Math.min(root.getElementCount(), (clip.y + clip.height + lineHeight - 1) / lineHeight);
            for (int i = startLine; i <= endLine; i++) {
                String num = String.valueOf(i);
                int x = getWidth() - MARGIN - fm.stringWidth(num);
                int y = (i - 1) * lineHeight + baseLine;
                g2.drawString(num, x, y);
            }
        } catch (Exception ignored) {}
    }
}
