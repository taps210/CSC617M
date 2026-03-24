package src.gui.editor;

import src.Scanner;
import src.Token;
import src.TokenType;
import src.errors.LexicalErrorRecord;
import src.gui.model.TokenColorMap;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Live syntax highlighting on keystroke. Runs the scanner only (no parser)
 * and applies TokenColorMap colors via StyledDocument.
 */
public class HerdSyntaxHighlighter implements DocumentListener {
    private final JTextPane editor;
    private final StyledDocument doc;
    private boolean updating;

    public HerdSyntaxHighlighter(JTextPane editor) {
        this.editor = editor;
        this.doc = editor.getStyledDocument();
        this.editor.getDocument().addDocumentListener(this);
    }

    @Override
    public void insertUpdate(DocumentEvent e) { rehighlight(); }

    @Override
    public void removeUpdate(DocumentEvent e) { rehighlight(); }

    @Override
    public void changedUpdate(DocumentEvent e) { }

    private void rehighlight() {
        if (updating) return;
        SwingUtilities.invokeLater(this::doHighlight);
    }

    private void doHighlight() {
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
            return;
        }
        if (text.isEmpty()) return;

        updating = true;
        try {
            // Clear existing token-based styles by setting default to plain
            SimpleAttributeSet plain = new SimpleAttributeSet();
            doc.setCharacterAttributes(0, doc.getLength(), plain, true);

            List<Token> tokens;
            try {
                Scanner scanner = new Scanner(text);
                // Use recovery mode so unknown symbols and similar recoverable errors
                // don't disable highlighting for the entire document.
                tokens = scanner.tokenizeAll(new ArrayList<LexicalErrorRecord>());
            } catch (Exception e) {
                // On lexical error (e.g. unterminated string), skip coloring rest
                return;
            }

            int[] lineStarts = ErrorHighlighter.buildLineStartOffsets(text);
            for (Token t : tokens) {
                if (t.type() == TokenType.EOF) continue;
                int line = t.line();
                int col = t.col();
                if (line < 1 || line > lineStarts.length) continue;
                int start = lineStarts[line - 1] + Math.max(0, col - 1);
                int len = t.lexeme() != null ? t.lexeme().length() : 0;
                int end = Math.min(start + len, doc.getLength());
                if (start >= doc.getLength() || start >= end) continue;
                Color color = TokenColorMap.getColor(t);
                SimpleAttributeSet set = new SimpleAttributeSet();
                StyleConstants.setForeground(set, color);
                doc.setCharacterAttributes(start, end - start, set, false);
            }
        } finally {
            updating = false;
        }
    }
}
