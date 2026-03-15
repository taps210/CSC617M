package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Terminal-styled output panel. Dark background, header shows run time,
 * footer shows completion status.
 */
public class InterpreterOutputPanel extends JPanel implements CompileListener {
    private static final String PLACEHOLDER = "(Run output will appear here after a successful compilation with no errors.)";
    private final PanelHeader header;
    private final JTextArea textArea;
    private final JLabel footerLabel;

    public InterpreterOutputPanel() {
        super(new BorderLayout());
        header = new PanelHeader();
        header.set("Output", Theme.PANEL_HEADER_NEUTRAL, "");

        textArea = new JTextArea(PLACEHOLDER);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setMargin(new Insets(6, 8, 6, 8));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        footerLabel = new JLabel(" ");
        footerLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        footerLabel.setForeground(Theme.LINE_NUMBER_FG);
        footerLabel.setBackground(Theme.PANEL_HEADER_BG);
        footerLabel.setOpaque(true);
        footerLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x3C3C3C)),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)
        ));

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(footerLabel, BorderLayout.SOUTH);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (result == null || result.interpreterOutput().isEmpty()) {
            header.set("Output", Theme.PANEL_HEADER_NEUTRAL, "");
            textArea.setText(PLACEHOLDER);
            footerLabel.setText(" ");
            textArea.setCaretPosition(0);
            return;
        }
        String output = result.interpreterOutput().get();
        String timeStr = PanelHeader.ms(result.metrics().runTimeNs);
        header.set("Output", Theme.PANEL_HEADER_OK, timeStr);
        if (output.isBlank()) {
            textArea.setText("");
            footerLabel.setText("[Program produced no output]");
        } else {
            textArea.setText(output);
            footerLabel.setText("[Program completed  " + timeStr + "]");
        }
        textArea.setCaretPosition(0);
    }
}
