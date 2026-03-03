package src.gui.editor;

import src.gui.model.CompileError;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.util.List;

/**
 * Manages error squiggle underlines on the editor. Clears and re-applies
 * highlights from a list of CompileErrors (line, col) after each compile.
 * Uses the document's Element model so highlights align with the view's line boundaries.
 */
public class ErrorHighlighter {
    private final JTextComponent component;
    private final Highlighter highlighter;
    private final SquigglePainter squigglePainter;
    private Object[] highlightTags = new Object[0];

    public ErrorHighlighter(JTextComponent component) {
        this.component = component;
        this.highlighter = component.getHighlighter();
        this.squigglePainter = new SquigglePainter(java.awt.Color.RED);
    }

    public void setErrors(List<CompileError> errors) {
        clear();
        if (errors == null || errors.isEmpty()) return;
        final List<CompileError> errList = new java.util.ArrayList<>(errors);
        javax.swing.SwingUtilities.invokeLater(() -> applyHighlights(errList));
    }

    private void applyHighlights(List<CompileError> errors) {
        for (Object tag : highlightTags) highlighter.removeHighlight(tag);
        highlightTags = new Object[0];
        if (errors.isEmpty()) return;
        Document doc = component.getDocument();
        Element root = doc.getDefaultRootElement();
        int docLen = doc.getLength();
        java.util.List<Object> tags = new java.util.ArrayList<>();
        for (CompileError err : errors) {
            int line = err.line();
            int lineIndex = line - 1;
            if (lineIndex < 0 || lineIndex >= root.getElementCount()) continue;
            Element lineEl = root.getElement(lineIndex);
            int start = lineEl.getStartOffset();
            int end = lineEl.getEndOffset();
            end = Math.min(end, docLen);
            if (start >= docLen) continue;
            if (start >= end) end = Math.min(start + 1, docLen);
            try {
                Object tag = highlighter.addHighlight(start, end, squigglePainter);
                tags.add(tag);
            } catch (BadLocationException ignored) {}
        }
        highlightTags = tags.toArray(new Object[0]);
        component.repaint();
    }

    public void clear() {
        for (Object tag : highlightTags) {
            highlighter.removeHighlight(tag);
        }
        highlightTags = new Object[0];
    }

    /** Build 0-based start offset for each line (1-based index). lineStarts[i] = offset of line i+1. */
    static int[] buildLineStartOffsets(String text) {
        java.util.List<Integer> starts = new java.util.ArrayList<>();
        starts.add(0);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') starts.add(i + 1);
        }
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }
}
