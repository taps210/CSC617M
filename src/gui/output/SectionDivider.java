package src.gui.output;

import src.gui.model.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * A horizontal rule where the title text breaks the line — like the CSS hr+span pattern.
 * Renders:  ──── Title ──────────────────────────────
 */
class SectionDivider extends JComponent {
    private final String title;

    SectionDivider(String title) {
        this.title = title;
        setPreferredSize(new Dimension(100, 22));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int w  = getWidth();
        int cy = getHeight() / 2;

        // Left segment: 8px gap, short fixed run, gap before text
        int leftLineEnd = 24;
        int textX       = leftLineEnd + 6;
        int textW       = fm.stringWidth(title);
        int rightLineStart = textX + textW + 6;

        g2.setColor(new Color(0x3C3C3C));
        g2.drawLine(8, cy, leftLineEnd, cy);
        g2.drawLine(rightLineStart, cy, w - 8, cy);

        g2.setColor(Theme.LINE_NUMBER_FG);
        g2.drawString(title, textX, cy + fm.getAscent() / 2);

        g2.dispose();
    }
}
