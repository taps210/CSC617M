package src.gui;

import src.gui.analysis.AnalysisWindow;
import src.gui.core.CompileController;
import src.gui.core.EditorFileHandler;
import src.gui.editor.EditorPanel;
import src.gui.output.OutputTabbedPane;
import src.gui.output.StatusBar;

import javax.swing.*;
import java.awt.*;

/**
 * Main IDE JFrame: menu, editor | output tabs, status bar. Wires CompileController to all panels.
 */
public class HerdIDE extends JFrame {
    private final CompileController controller;
    private final EditorPanel editorPanel;
    private final OutputTabbedPane outputTabs;
    private final StatusBar statusBar;
    private final AnalysisWindow analysisWindow;
    private final EditorFileHandler fileHandler;

    public HerdIDE() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);

        controller = new CompileController();
        editorPanel = new EditorPanel(controller, this::openAnalysis);
        outputTabs = new OutputTabbedPane();
        statusBar = new StatusBar();
        analysisWindow = new AnalysisWindow();

        controller.addListener(editorPanel);
        controller.addListener(outputTabs);
        controller.addListener(statusBar);

        statusBar.attachToEditor(editorPanel.getEditor());
        outputTabs.getErrorsPanel().setOnErrorSelected(() -> {
            var err = outputTabs.getErrorsPanel().getSelectedError();
            if (err != null) editorPanel.setCaretToLineAndColumn(err.line(), err.col());
        });

        fileHandler = new EditorFileHandler(this, editorPanel::getText, editorPanel::setText, this::updateWindowTitle);
        updateWindowTitle();

        buildMenuBar();
        JPanel content = new JPanel(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, outputTabs);
        split.setResizeWeight(0.6);
        content.add(split, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void updateWindowTitle() {
        var f = fileHandler.getCurrentFile();
        setTitle(f == null ? "Herd IDE" : "Herd IDE - " + f.getName());
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        openItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        openItem.addActionListener(e -> fileHandler.open());
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(e -> fileHandler.save());
        JMenuItem saveAsItem = new JMenuItem("Save As");
        saveAsItem.addActionListener(e -> fileHandler.saveAs());
        JMenuItem runItem = new JMenuItem("Run");
        runItem.setAccelerator(KeyStroke.getKeyStroke("control ENTER"));
        runItem.addActionListener(e -> editorPanel.run());
        JMenuItem analyzeItem = new JMenuItem("Analyze");
        analyzeItem.addActionListener(e -> openAnalysis());
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(runItem);
        fileMenu.add(analyzeItem);
        bar.add(fileMenu);
        setJMenuBar(bar);
    }

    private void openAnalysis() {
        if (controller.getLastResult() == null) return;
        analysisWindow.open(controller.getLastResult());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            HerdIDE ide = new HerdIDE();
            ide.setLocationRelativeTo(null);
            ide.setVisible(true);
        });
    }
}
