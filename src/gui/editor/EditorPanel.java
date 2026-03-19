package src.gui.editor;

import src.gui.core.CompileController;
import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
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
    }

    public void run() {
        errorHighlighter.clear();
        beforeRunHook.run();
        requestCompile(editor.getText());
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
}
