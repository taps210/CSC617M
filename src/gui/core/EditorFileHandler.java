package src.gui.core;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles open, save, and save-as for the editor using .hd files.
 * Uses the parent frame for dialogs and notifies when the current file changes.
 */
public class EditorFileHandler {
    private static final String HD_EXTENSION = "hd";
    private static final String HD_FILTER_DESC = "Herd source (*.hd)";

    private final JFrame parent;
    private final Supplier<String> getText;
    private final Consumer<String> setText;
    private final Runnable onFileChanged;
    private File currentFile;

    public EditorFileHandler(JFrame parent, Supplier<String> getText, Consumer<String> setText, Runnable onFileChanged) {
        this.parent = parent;
        this.getText = getText;
        this.setText = setText;
        this.onFileChanged = onFileChanged;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void open() {
        File f = showFileChooser(JFileChooser.OPEN_DIALOG);
        if (f == null) return;
        try {
            String content = Files.readString(f.toPath());
            setText.accept(content);
            currentFile = f;
            onFileChanged.run();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Could not open file: " + ex.getMessage(), "Open failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void save() {
        if (currentFile != null) {
            doSaveTo(currentFile);
            return;
        }
        File f = showFileChooser(JFileChooser.SAVE_DIALOG);
        if (f == null) return;
        if (doSaveTo(f)) {
            currentFile = f;
            onFileChanged.run();
        }
    }

    public void saveAs() {
        File f = showFileChooser(JFileChooser.SAVE_DIALOG);
        if (f == null) return;
        if (doSaveTo(f)) {
            currentFile = f;
            onFileChanged.run();
        }
    }

    private File showFileChooser(int mode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(HD_FILTER_DESC, HD_EXTENSION));
        int result = mode == JFileChooser.SAVE_DIALOG
            ? chooser.showSaveDialog(parent)
            : chooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return null;
        File f = chooser.getSelectedFile();
        if (f != null && mode == JFileChooser.SAVE_DIALOG && !f.getName().toLowerCase().endsWith("." + HD_EXTENSION)) {
            f = new File(f.getParent(), f.getName() + "." + HD_EXTENSION);
        }
        return f;
    }

    private boolean doSaveTo(File f) {
        try {
            Files.writeString(f.toPath(), getText.get());
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Could not save file: " + ex.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
