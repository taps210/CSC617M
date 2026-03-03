package src.gui.editor;

import src.gui.core.CompileController;
import src.gui.core.CompileListener;
import src.gui.core.CompileResult;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.*;

/**
 * Editor panel: JTextPane with syntax highlighting, error underlines,
 * line numbers, and Run / Analyze toolbar.
 */
public class EditorPanel extends JPanel implements CompileListener {
    private final JTextPane editor;
    private final CompileController controller;
    private final ErrorHighlighter errorHighlighter;
    private final JButton runButton;
    private final JButton analyzeButton;
    private final Runnable onAnalyzeClicked;

    public EditorPanel(CompileController controller, Runnable onAnalyzeClicked) {
        super(new BorderLayout());
        this.controller = controller;
        this.onAnalyzeClicked = onAnalyzeClicked;
        this.editor = new JTextPane();
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setMargin(new Insets(4, 4, 4, 4));
        new HerdSyntaxHighlighter(editor);
        this.errorHighlighter = new ErrorHighlighter(editor);

        JScrollPane scroll = new JScrollPane(editor);
        LineNumberComponent lineNumbers = new LineNumberComponent(editor);
        scroll.setRowHeaderView(lineNumbers);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        SwingUtilities.invokeLater(() -> {
            scroll.getRowHeader().setPreferredSize(new Dimension(Math.max(36, lineNumbers.getPreferredSize().width), 0));
            scroll.revalidate();
        });

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        runButton = new JButton("Run");
        runButton.setToolTipText("Run tokenizer and parser (Ctrl+Enter)");
        runButton.addActionListener(e -> run());
        analyzeButton = new JButton("Analyze");
        analyzeButton.setToolTipText("Open analysis window (requires at least one run)");
        analyzeButton.setEnabled(false);
        analyzeButton.addActionListener(e -> {
            if (onAnalyzeClicked != null) onAnalyzeClicked.run();
        });
        toolbar.add(runButton);
        toolbar.add(analyzeButton);

        add(toolbar, BorderLayout.NORTH);
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
        controller.compile(editor.getText());
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        errorHighlighter.setErrors(result.errors());
        analyzeButton.setEnabled(true);
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
