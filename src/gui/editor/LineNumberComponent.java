package src.gui.editor;

import src.gui.core.BreakpointListener;
import src.gui.model.Theme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Paints line numbers in the row header of the editor scroll pane.
 * Supports breakpoint display and click handling.
 */
public class LineNumberComponent extends JComponent {
    private final JTextPane editor;
    private static final int MARGIN = 16;  // Expanded to accommodate breakpoint circles
    private static final int MIN_WIDTH = 52;  // Expanded for circles
    private static final int BREAKPOINT_RADIUS = 4;
    private static final Color BREAKPOINT_COLOR = new Color(0xE0, 0x6C, 0x75);  // Red

    private final Set<Integer> breakpoints = new CopyOnWriteArraySet<>();
    private final List<BreakpointListener> bpListeners = new ArrayList<>();

    public LineNumberComponent(JTextPane editor) {
        this.editor = editor;
        setFont(editor.getFont());
        setBackground(Theme.LINE_NUMBER_BG);
        setForeground(Theme.LINE_NUMBER_FG);
        updatePreferredWidth();
        setMinimumSize(new Dimension(MIN_WIDTH, 0));

        // Add mouse listener for breakpoint clicks
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleBreakpointClick(e);
            }
        });

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updatePreferredWidth(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updatePreferredWidth(); }
            @Override
            public void changedUpdate(DocumentEvent e) { }
        });
    }

    public void addBreakpointListener(BreakpointListener listener) {
        bpListeners.add(listener);
    }

    public Set<Integer> getBreakpoints() {
        return Set.copyOf(breakpoints);
    }

    private void handleBreakpointClick(MouseEvent e) {
        FontMetrics fm = getFontMetrics(getFont());
        if (fm == null) return;
        int lineHeight = fm.getHeight();
        int clickedLine = (e.getY() / lineHeight) + 1;
        if (clickedLine < 1) return;

        // Toggle breakpoint
        if (breakpoints.contains(clickedLine)) {
            breakpoints.remove(clickedLine);
        } else {
            breakpoints.add(clickedLine);
        }

        // Notify listeners
        for (BreakpointListener listener : bpListeners) {
            listener.onBreakpointsChanged(getBreakpoints());
        }

        repaint();
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
                // Draw breakpoint circle if set
                if (breakpoints.contains(i)) {
                    int circleX = 6 + BREAKPOINT_RADIUS;
                    int circleY = (i - 1) * lineHeight + lineHeight / 2;
                    g2.setColor(BREAKPOINT_COLOR);
                    g2.fillOval(circleX - BREAKPOINT_RADIUS, circleY - BREAKPOINT_RADIUS,
                               BREAKPOINT_RADIUS * 2, BREAKPOINT_RADIUS * 2);
                    g2.setColor(getForeground());
                }

                // Draw line number
                String num = String.valueOf(i);
                int x = getWidth() - MARGIN - fm.stringWidth(num);
                int y = (i - 1) * lineHeight + baseLine;
                g2.drawString(num, x, y);
            }
        } catch (Exception ignored) {}
    }
}
