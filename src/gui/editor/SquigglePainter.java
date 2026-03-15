package src.gui.editor;

import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.geom.Rectangle2D;

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
            Rectangle2D r0 = c.modelToView2D(p0);
            Rectangle2D r1 = c.modelToView2D(p1);
            if (r0 == null) return;
            int y = (int)(r0.getY() + r0.getHeight()) - 1;
            int x1 = (int) r0.getX();
            int x2 = (r1 != null && (int) r1.getY() == (int) r0.getY()) ? (int) r1.getX() : bounds.getBounds().x + bounds.getBounds().width;
            if (x2 <= x1) x2 = x1 + 4;
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
