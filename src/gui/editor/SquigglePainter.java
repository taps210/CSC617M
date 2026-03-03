package src.gui.editor;

import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Draws a red wavy underline beneath highlighted text (e.g. for errors).
 */
public class SquigglePainter implements Highlighter.HighlightPainter {
    private static final int WAVE_AMPLITUDE = 2;
    private static final int WAVE_PERIOD = 4;

    private final Color color;

    public SquigglePainter(Color color) {
        this.color = color;
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
        try {
            Rectangle r = bounds.getBounds();
            int y = r.y + r.height - 1;
            int x1 = r.x;
            int x2 = r.x + r.width;
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            int steps = Math.max(1, (x2 - x1) / WAVE_PERIOD);
            int prevX = x1;
            int prevY = y;
            for (int i = 1; i <= steps; i++) {
                int x = x1 + (x2 - x1) * i / steps;
                int yy = y + (i % 2 == 0 ? WAVE_AMPLITUDE : -WAVE_AMPLITUDE);
                g2.drawLine(prevX, prevY, x, yy);
                prevX = x;
                prevY = yy;
            }
            if (prevX < x2)
                g2.drawLine(prevX, prevY, x2, y);
        } catch (Exception ignored) {}
    }
}
