package src.gui.output;

import src.gui.model.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Thin header strip shown above each output panel.
 * Displays a colored status badge on the left and a stats string on the right.
 */
class PanelHeader extends JPanel {
    private final JLabel badgeLabel;
    private final JLabel statsLabel;

    PanelHeader() {
        setLayout(new BorderLayout());
        setBackground(Theme.PANEL_HEADER_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x3C3C3C)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        badgeLabel = new JLabel("—");
        statsLabel = new JLabel("");
        badgeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statsLabel.setForeground(Theme.LINE_NUMBER_FG);
        add(badgeLabel, BorderLayout.WEST);
        add(statsLabel, BorderLayout.EAST);
    }

    void set(String badge, Color badgeColor, String stats) {
        badgeLabel.setText(badge);
        badgeLabel.setForeground(badgeColor);
        statsLabel.setText(stats);
    }

    static String ms(long nanos) {
        return String.format("%.1fms", nanos / 1_000_000.0);
    }
}
