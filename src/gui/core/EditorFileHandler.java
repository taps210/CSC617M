package src.gui.core;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handles open, save, and save-as for the editor using .hd files.
 * open() opens a file in a new tab via openInTab callback.
 * save()/saveAs() write the active tab's content to disk.
 */
public class EditorFileHandler {
    private static final String HD_EXTENSION = "hd";
    private static final String HD_FILTER_DESC = "Herd source (*.hd)";

    private final JFrame parent;
    private final Supplier<String> getText;
    private final BiConsumer<File, String> openInTab;
    private final Supplier<File> getCurrentFile;
    private final Consumer<File> setCurrentFile;
    private final Runnable onFileChanged;
    private File lastDirectory;

    public EditorFileHandler(JFrame parent,
                              Supplier<String> getText,
                              BiConsumer<File, String> openInTab,
                              Supplier<File> getCurrentFile,
                              Consumer<File> setCurrentFile,
                              Runnable onFileChanged) {
        this.parent = parent;
        this.getText = getText;
        this.openInTab = openInTab;
        this.getCurrentFile = getCurrentFile;
        this.setCurrentFile = setCurrentFile;
        this.onFileChanged = onFileChanged;
    }

    public void open() {
        File f = showFileChooser(JFileChooser.OPEN_DIALOG);
        if (f == null) return;
        try {
            String content = Files.readString(f.toPath());
            openInTab.accept(f, content);
            onFileChanged.run();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Could not open file: " + ex.getMessage(), "Open failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void save() {
        File f = getCurrentFile.get();
        if (f != null) {
            if (doSaveTo(f)) onFileChanged.run();
            return;
        }
        File dest = showFileChooser(JFileChooser.SAVE_DIALOG);
        if (dest == null) return;
        if (doSaveTo(dest)) {
            setCurrentFile.accept(dest);
            onFileChanged.run();
        }
    }

    public void saveAs() {
        File dest = showFileChooser(JFileChooser.SAVE_DIALOG);
        if (dest == null) return;
        if (doSaveTo(dest)) {
            setCurrentFile.accept(dest);
            onFileChanged.run();
        }
    }

    private File showFileChooser(int mode) {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileFilter(new FileNameExtensionFilter(HD_FILTER_DESC, HD_EXTENSION));
        int result = mode == JFileChooser.SAVE_DIALOG
            ? chooser.showSaveDialog(parent)
            : chooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return null;
        File f = chooser.getSelectedFile();
        if (f != null && mode == JFileChooser.SAVE_DIALOG && !f.getName().toLowerCase().endsWith("." + HD_EXTENSION)) {
            f = new File(f.getParent(), f.getName() + "." + HD_EXTENSION);
        }
        if (f != null) lastDirectory = f.getParentFile();
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
