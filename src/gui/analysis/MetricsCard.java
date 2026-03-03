package src.gui.analysis;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reusable panel with titled border and key-value rows (label : value).
 */
public class MetricsCard extends JPanel {
    public MetricsCard(String title, LinkedHashMap<String, String> entries) {
        setBorder(BorderFactory.createTitledBorder(title));
        setLayout(new GridLayout(entries.size(), 2, 4, 4));
        for (Map.Entry<String, String> e : entries.entrySet()) {
            add(new JLabel(e.getKey()));
            JLabel val = new JLabel(e.getValue());
            val.setHorizontalAlignment(SwingConstants.RIGHT);
            add(val);
        }
    }
}
