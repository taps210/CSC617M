package src.gui.debug;

import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

/**
 * Manages yellow highlight for the current debug execution line.
 */
public class DebugLineHighlighter {
    private final JTextPane editor;
    private final Color HIGHLIGHT_COLOR = new Color(0xFF, 0xFF, 0x99);  // Yellow
    private int highlightedLineStart = -1;
    private int highlightedLineEnd = -1;

    public DebugLineHighlighter(JTextPane editor) {
        this.editor = editor;
    }

    public void highlightLine(int line) {
        // Clear previous highlight first
        if (highlightedLineStart >= 0 && highlightedLineEnd >= 0) {
            clear();
        }

        // Get the line's text boundaries
        StyledDocument doc = editor.getStyledDocument();
        try {
            // Convert 1-based line number to 0-based
            javax.swing.text.Element root = doc.getDefaultRootElement();
            if (line < 1 || line > root.getElementCount()) return;

            javax.swing.text.Element lineElement = root.getElement(line - 1);
            int startOffset = lineElement.getStartOffset();
            int endOffset = lineElement.getEndOffset();

            // Apply yellow background
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setBackground(attrs, HIGHLIGHT_COLOR);
            doc.setCharacterAttributes(startOffset, endOffset - startOffset, attrs, false);

            highlightedLineStart = startOffset;
            highlightedLineEnd = endOffset;
        } catch (Exception e) {
            // Line doesn't exist or other error - silently ignore
        }
    }

    public void clear() {
        if (highlightedLineStart < 0 || highlightedLineEnd < 0) return;

        StyledDocument doc = editor.getStyledDocument();
        try {
            // Create empty attributes to remove background
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            doc.setCharacterAttributes(highlightedLineStart, highlightedLineEnd - highlightedLineStart, attrs, false);
        } catch (Exception e) {
            // Silently ignore
        }

        highlightedLineStart = -1;
        highlightedLineEnd = -1;
    }
}
