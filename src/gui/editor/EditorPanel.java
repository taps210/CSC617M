package src.gui.editor;

import src.gui.core.CompileController;
import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.debug.DebugLineHighlighter;
import src.gui.model.Theme;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.*;
import java.util.Objects;

/**
 * Editor panel: JTextPane with syntax highlighting, error underlines, and line numbers.
 * Run/Analyze actions are exposed via run() and are triggered from HerdIDE's tab bar.
 */
public class EditorPanel extends JPanel implements CompileListener {
    private final JTextPane editor;
    private final CompileController controller;
    private final ErrorHighlighter errorHighlighter;
    private final DebugLineHighlighter debugLineHighlighter;
    private Runnable beforeRunHook = () -> {};
    private boolean compiling;
    private boolean pendingCompile;
    private String pendingSource = "";

    public EditorPanel(CompileController controller) {
        super(new BorderLayout());
        this.controller = controller;
        this.editor = new JTextPane();
        editor.setFont(Theme.EDITOR_FONT);
        editor.setBackground(Theme.EDITOR_BG);
        editor.setForeground(Theme.EDITOR_FG);
        editor.setCaretColor(Theme.EDITOR_CARET);
        editor.setSelectionColor(Theme.EDITOR_SELECTION);
        editor.setMargin(new Insets(4, 4, 4, 4));
        new HerdSyntaxHighlighter(editor);
        this.errorHighlighter = new ErrorHighlighter(editor);
        this.debugLineHighlighter = new DebugLineHighlighter(editor);
        new HoverTooltipManager(editor, controller);

        JScrollPane scroll = new JScrollPane(editor);
        LineNumberComponent lineNumbers = new LineNumberComponent(editor);
        scroll.setRowHeaderView(lineNumbers);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        SwingUtilities.invokeLater(() -> {
            scroll.getRowHeader().setPreferredSize(new Dimension(Math.max(36, lineNumbers.getPreferredSize().width), 0));
            scroll.revalidate();
        });

        add(scroll, BorderLayout.CENTER);

        // Ctrl+Enter to run
        editor.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("control ENTER"), "run");
        editor.getActionMap().put("run", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { run(); }
        });

        // Cmd+/ (Mac) or Ctrl+/ to toggle line comments
        editor.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SLASH,
                        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "toggleComment");
        editor.getActionMap().put("toggleComment", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) { toggleLineComment(); }
        });
    }

    public void run() {
        errorHighlighter.clear();
        beforeRunHook.run();
        requestCompile(editor.getText());
    }

    private void toggleLineComment() {
        javax.swing.text.Document doc = editor.getDocument();
        String text = editor.getText();
        int selStart = editor.getSelectionStart();
        int selEnd = editor.getSelectionEnd();

        // Find the line boundaries for the selection
        int lineStart = text.lastIndexOf('\n', selStart - 1) + 1;
        int lineEnd = text.indexOf('\n', selEnd);
        if (lineEnd == -1) lineEnd = text.length();

        // Get all selected lines
        String[] lines = text.substring(lineStart, lineEnd).split("\n", -1);

        // Determine if we should comment or uncomment: if ALL lines are commented, uncomment
        boolean allCommented = true;
        for (String line : lines) {
            String trimmed = line.stripLeading();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                allCommented = false;
                break;
            }
        }

        // Build the replacement text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            if (allCommented) {
                // Uncomment: remove first occurrence of "// " or "//"
                int idx = lines[i].indexOf("//");
                if (idx >= 0) {
                    String after = lines[i].substring(idx + 2);
                    if (after.startsWith(" ")) after = after.substring(1);
                    sb.append(lines[i], 0, idx).append(after);
                } else {
                    sb.append(lines[i]);
                }
            } else {
                // Comment: add "// " at the start of the line
                sb.append("// ").append(lines[i]);
            }
        }

        // Replace the text
        try {
            doc.remove(lineStart, lineEnd - lineStart);
            doc.insertString(lineStart, sb.toString(), null);
            // Restore selection over the modified lines
            editor.setSelectionStart(lineStart);
            editor.setSelectionEnd(lineStart + sb.length());
        } catch (javax.swing.text.BadLocationException ex) {
            // ignore
        }
    }

    public void setBeforeRunHook(Runnable beforeRunHook) {
        this.beforeRunHook = Objects.requireNonNullElse(beforeRunHook, () -> {});
    }

    private synchronized void requestCompile(String source) {
        if (compiling) {
            pendingCompile = true;
            pendingSource = source != null ? source : "";
            return;
        }
        compiling = true;
        startCompileWorker(source != null ? source : "");
    }

    private void startCompileWorker(String source) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                controller.compileInteractive(source);
                return null;
            }

            @Override
            protected void done() {
                String nextSource;
                synchronized (EditorPanel.this) {
                    compiling = false;
                    if (!pendingCompile) return;
                    pendingCompile = false;
                    nextSource = pendingSource;
                }
                requestCompile(nextSource);
            }
        };
        worker.execute();
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        errorHighlighter.setErrors(result.errors());
    }

    public JTextPane getEditor() { return editor; }

    public String getText() { return editor.getText(); }

    public void setText(String text) {
        editor.setText(text != null ? text : "");
        errorHighlighter.clear();
    }

    public void setCaretToLine(int line) {
        try {
            javax.swing.text.Document doc = editor.getDocument();
            if (line < 1) return;
            Element root = doc.getDefaultRootElement();
            int index = line <= root.getElementCount() ? root.getElement(line - 1).getStartOffset() : doc.getLength();
            editor.setCaretPosition(Math.min(index, doc.getLength()));
            editor.requestFocusInWindow();
        } catch (Exception ignored) {}
    }

    public void setCaretToLineAndColumn(int line, int col) {
        try {
            String text = editor.getText();
            int[] starts = ErrorHighlighter.buildLineStartOffsets(text);
            if (line < 1 || line > starts.length) return;
            int offset = starts[line - 1] + Math.max(0, col - 1);
            offset = Math.min(offset, editor.getDocument().getLength());
            editor.setCaretPosition(offset);
            editor.requestFocusInWindow();
        } catch (Exception ignored) {}
    }

    public void highlightDebugLine(int line) {
        debugLineHighlighter.highlightLine(line);
    }

    public void clearDebugHighlight() {
        debugLineHighlighter.clear();
    }

    public LineNumberComponent getLineNumberComponent() {
        // Get line number component from scroll pane row header
        // This is a temporary implementation - the actual component is stored in the scroll pane
        JScrollPane scroll = (JScrollPane) getComponent(0);
        if (scroll != null && scroll.getRowHeader().getView() instanceof LineNumberComponent lnc) {
            return lnc;
        }
        return null;
    }
}
