package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.model.CompileError;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Read-only panel showing semantic analysis status and report.
 * Shows OK, "not run" when no parse tree, or error count plus list of semantic errors.
 */
public class SemanticOutputPanel extends JScrollPane implements CompileListener {
    private static final String PLACEHOLDER = "(Semantic analysis output will appear here after Run.)";
    private final JTextArea textArea;

    public SemanticOutputPanel() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setMargin(new Insets(4, 4, 4, 4));
        setViewportView(textArea);
        textArea.setText(PLACEHOLDER);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (result == null) {
            textArea.setText(PLACEHOLDER);
            textArea.setCaretPosition(0);
            return;
        }
        List<CompileError> semanticErrors = result.errors().stream()
                .filter(e -> e.source() == CompileError.Source.SEMANTIC)
                .toList();
        int count = semanticErrors.size();
        boolean astPresent = result.ast().isPresent();

        StringBuilder sb = new StringBuilder();
        if (!astPresent) {
            sb.append("Semantic analysis not run (parse failed or no parse tree).");
        } else if (count > 0) {
            sb.append("Semantic analysis found ").append(count).append(" error(s). See Errors tab for details.")
              .append(System.lineSeparator()).append(System.lineSeparator());
            for (CompileError e : semanticErrors) {
                sb.append("  L").append(e.line()).append(": ").append(e.message()).append(System.lineSeparator());
            }
        } else {
            sb.append("Semantic analysis: OK.");
        }
        String report = sb.toString();
        textArea.setText(report.isEmpty() ? PLACEHOLDER : report);
        textArea.setCaretPosition(0);
    }
}
