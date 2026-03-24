package src.gui;

import src.gui.core.CompileController;
import src.gui.core.EditorFileHandler;
import src.gui.debug.DebugController;
import src.gui.editor.EditorPanel;
import src.gui.output.OutputTabbedPane;
import src.gui.output.StatusBar;

import com.formdev.flatlaf.FlatDarkLaf;
import src.gui.model.Theme;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main IDE JFrame: editor tabs | output tabs, status bar.
 * No separate menu bar — File/Run/Analyze live as action tabs in the editor tab strip.
 */
public class HerdIDE extends JFrame {
    private static final int TAB_FILE   = 0;
    private static final int TAB_EDITOR = 1;
    private static final int TAB_RUN    = 2;
    private static final int TAB_DEBUG  = 3;

    private final CompileController controller;
    private final EditorPanel editorPanel;
    private final OutputTabbedPane outputTabs;
    private final StatusBar statusBar;
    private final EditorFileHandler fileHandler;
    private final DebugController debugController;

    public HerdIDE() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setApplicationIcon();

        controller = new CompileController();
        editorPanel = new EditorPanel(controller);
        outputTabs = new OutputTabbedPane();
        statusBar = new StatusBar();
        debugController = new DebugController(editorPanel, outputTabs.getDebuggerPanel());

        controller.addListener(editorPanel);
        controller.addListener(outputTabs);
        controller.addListener(statusBar);
        controller.addListener(debugController);  // Register debugger as compile listener
        editorPanel.getLineNumberComponent().addBreakpointListener(debugController);  // Register breakpoint listener

        // Wire debugger panel setups
        outputTabs.getDebuggerPanel().setEditorPanel(editorPanel);
        outputTabs.getDebuggerPanel().setLineNumberComponent(editorPanel.getLineNumberComponent());
        outputTabs.getDebuggerPanel().setOnStepOverRequested(debugController::stepOver);
        outputTabs.getDebuggerPanel().setOnStepIntoRequested(debugController::stepInto);
        outputTabs.getDebuggerPanel().setOnStepOutRequested(debugController::stepOut);
        outputTabs.getDebuggerPanel().setOnContinueRequested(debugController::resume);
        outputTabs.getDebuggerPanel().setOnStopRequested(debugController::stopDebugSession);

        controller.setRuntimeEventListener(outputTabs.getInterpreterPanel());
        controller.addListener(debugController);  // Register as runtime event listener

        editorPanel.setBeforeRunHook(() -> outputTabs.getInterpreterPanel().configureInputPrompts(editorPanel.getText()));
        outputTabs.getInterpreterPanel().setOnStartRequested(editorPanel::run);
        outputTabs.getInterpreterPanel().setOnRestartRequested(() -> {
            controller.stopRuntime();
            editorPanel.run();
        });
        outputTabs.getInterpreterPanel().setOnStopRequested(controller::stopRuntime);
        outputTabs.getInterpreterPanel().setOnInputSubmitted(line -> {
            if (!controller.isRuntimeActive() && !controller.isCompiling()) editorPanel.run();
            controller.submitRuntimeInputLine(line);
        });

        statusBar.attachToEditor(editorPanel.getEditor());
        outputTabs.getErrorsPanel().setOnErrorSelected(() -> {
            var err = outputTabs.getErrorsPanel().getSelectedError();
            if (err != null) editorPanel.setCaretToLineAndColumn(err.line(), err.col());
        });

        fileHandler = new EditorFileHandler(this, editorPanel::getText, editorPanel::setText, this::updateWindowTitle);
        updateWindowTitle();

        JTabbedPane editorTabs = new JTabbedPane();
        editorTabs.addTab("File",   new JPanel());
        editorTabs.addTab("Editor", editorPanel);
        editorTabs.addTab("Run",    new JPanel());
        editorTabs.addTab("Debug",  new JPanel());
        editorTabs.setSelectedIndex(TAB_EDITOR);

        editorTabs.addChangeListener(e -> {
            int sel = editorTabs.getSelectedIndex();
            if (sel == TAB_FILE) {
                Rectangle bounds = editorTabs.getBoundsAt(TAB_FILE);
                showFileMenu(editorTabs, bounds.x, bounds.y + bounds.height);
                SwingUtilities.invokeLater(() -> editorTabs.setSelectedIndex(TAB_EDITOR));
            } else if (sel == TAB_RUN) {
                editorPanel.run();
                SwingUtilities.invokeLater(() -> editorTabs.setSelectedIndex(TAB_EDITOR));
            } else if (sel == TAB_DEBUG) {
                var irFuncs = controller.getLastIrFuncs();
                var ast = controller.getLastAst();
                if (irFuncs != null && ast != null) {
                    debugController.startDebugSession(irFuncs, ast);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(HerdIDE.this,
                        "Please compile successfully before debugging.");
                }
                SwingUtilities.invokeLater(() -> editorTabs.setSelectedIndex(TAB_EDITOR));
            }
        });

        registerKeyboardShortcuts();

        JPanel content = new JPanel(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorTabs, outputTabs);
        split.setResizeWeight(0.6);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.6));
        content.add(split, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        setContentPane(content);
    }

    private void setApplicationIcon() {
        Image icon = loadIcon("assets/herd_logo_v2_chatgpt.png");
        if (icon == null) return;

        setIconImage(icon);
        setIconImages(makeIconSizes(icon));
        applyTaskbarIcon(icon);
    }

    private Image loadIcon(String relativePath) {
        for (URL url : iconCandidates(relativePath)) {
            try {
                Image img = ImageIO.read(url);
                if (img != null) return img;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private List<URL> iconCandidates(String relativePath) {
        List<URL> urls = new ArrayList<>();

        // 1) Packaged resource (works when assets are on the classpath).
        URL resource = getClass().getClassLoader().getResource(relativePath);
        if (resource != null) urls.add(resource);

        // 2) Common dev run locations (works when running from IDE/terminal).
        for (Path p : fileCandidates(relativePath)) {
            try {
                File f = p.toFile();
                if (f.exists()) urls.add(f.toURI().toURL());
            } catch (Exception ignored) {
            }
        }
        return urls;
    }

    private List<Path> fileCandidates(String relativePath) {
        List<Path> paths = new ArrayList<>();
        Path cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();

        // Try cwd, then a few parents (handles running from build/ or src/).
        Path base = cwd;
        for (int i = 0; i < 6 && base != null; i++) {
            paths.add(base.resolve(relativePath).normalize());
            base = base.getParent();
        }
        return paths;
    }

    private List<Image> makeIconSizes(Image icon) {
        return List.of(
            icon.getScaledInstance(16, 16, Image.SCALE_SMOOTH),
            icon.getScaledInstance(32, 32, Image.SCALE_SMOOTH),
            icon.getScaledInstance(64, 64, Image.SCALE_SMOOTH),
            icon.getScaledInstance(128, 128, Image.SCALE_SMOOTH)
        );
    }

    private void applyTaskbarIcon(Image icon) {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(icon);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void showFileMenu(JComponent anchor, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem   = new JMenuItem("Open        Ctrl+O");
        JMenuItem saveItem   = new JMenuItem("Save        Ctrl+S");
        JMenuItem saveAsItem = new JMenuItem("Save As");
        openItem.addActionListener(e -> fileHandler.open());
        saveItem.addActionListener(e -> fileHandler.save());
        saveAsItem.addActionListener(e -> fileHandler.saveAs());
        menu.add(openItem);
        menu.add(saveItem);
        menu.add(saveAsItem);
        menu.show(anchor, x, y);
    }

    private void registerKeyboardShortcuts() {
        JRootPane root = getRootPane();
        InputMap  im   = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am   = root.getActionMap();
        im.put(KeyStroke.getKeyStroke("control O"), "file-open");
        im.put(KeyStroke.getKeyStroke("control S"), "file-save");
        im.put(KeyStroke.getKeyStroke("control ENTER"), "run");
        im.put(KeyStroke.getKeyStroke("F5"), "debug");
        am.put("file-open", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { fileHandler.open(); }
        });
        am.put("file-save", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { fileHandler.save(); }
        });
        am.put("run", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { editorPanel.run(); }
        });
        am.put("debug", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                var irFuncs = controller.getLastIrFuncs();
                var ast = controller.getLastAst();
                if (irFuncs != null && ast != null) {
                    debugController.startDebugSession(irFuncs, ast);
                } else {
                    javax.swing.JOptionPane.showMessageDialog(HerdIDE.this,
                        "Please compile successfully before debugging.");
                }
            }
        });
    }

    private void updateWindowTitle() {
        var f = fileHandler.getCurrentFile();
        setTitle(f == null ? "Herd IDE" : "Herd IDE — " + f.getName());
        outputTabs.getInterpreterPanel().loadSiblingInputPreset(f != null ? f.toPath() : null);
    }

    // Source - https://stackoverflow.com/a/56961097
    // Posted by George Z., modified by community. License - CC BY-SA 4.0
    // Temporarily flips Frame's private "undecorated" field to bypass setOpacity's
    // decoration guard, then restores it so native title bar behavior is unaffected.
    private static void applyOpacity(Frame frame, float opacity) {
        if (!GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) return;
        try {
            Field f = Frame.class.getDeclaredField("undecorated");
            f.setAccessible(true);
            f.set(frame, true);
            frame.setOpacity(opacity);
            f.set(frame, false);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatDarkLaf.setup();
            } catch (Exception ignored) {}
            HerdIDE ide = new HerdIDE();
            ide.setLocationRelativeTo(null);
            ide.setVisible(true);
            applyOpacity(ide, Theme.WINDOW_OPACITY);
        });
    }
}
