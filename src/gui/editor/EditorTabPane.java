package src.gui.editor;

import src.gui.core.BreakpointListener;
import src.gui.core.CompileController;
import src.gui.core.CompileListener;
import src.gui.core.CompileResult;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabbed container of EditorPanels. Each tab holds one file.
 * Tabs have close (×) buttons and a dirty (•) indicator in the title.
 */
public class EditorTabPane extends JPanel implements CompileListener {

    private final JTabbedPane tabs = new JTabbedPane();
    private final CompileController controller;
    private final List<TabEntry> entries = new ArrayList<>();
    private final List<Runnable> changeListeners = new ArrayList<>();
    private BreakpointListener breakpointListener;
    private Runnable beforeRunHook;

    private static final class TabEntry {
        final EditorPanel panel;
        File file;
        boolean dirty;

        TabEntry(EditorPanel panel, File file) {
            this.panel = panel;
            this.file = file;
        }

        String displayName() {
            String name = file != null ? file.getName() : "untitled.hd";
            return dirty ? name + " \u2022" : name;
        }
    }

    public EditorTabPane(CompileController controller) {
        super(new BorderLayout());
        this.controller = controller;
        add(tabs, BorderLayout.CENTER);
        addEmptyTab();
        tabs.addChangeListener(e -> changeListeners.forEach(Runnable::run));
    }

    // ── Public API ──────────────────────────────────────────────────────────

    public EditorPanel getActivePanel() {
        int i = tabs.getSelectedIndex();
        return (i >= 0 && i < entries.size()) ? entries.get(i).panel : null;
    }

    public File getActiveFile() {
        int i = tabs.getSelectedIndex();
        return (i >= 0 && i < entries.size()) ? entries.get(i).file : null;
    }

    public void setActiveFile(File f) {
        int i = tabs.getSelectedIndex();
        if (i < 0 || i >= entries.size()) return;
        entries.get(i).file = f;
        entries.get(i).dirty = false;
        refreshTitle(i);
    }

    public String getActiveText() {
        EditorPanel p = getActivePanel();
        return p != null ? p.getText() : "";
    }

    public void setActiveText(String text) {
        EditorPanel p = getActivePanel();
        if (p != null) p.setText(text);
    }

    public void markActiveClean() {
        int i = tabs.getSelectedIndex();
        if (i >= 0 && i < entries.size()) {
            entries.get(i).dirty = false;
            refreshTitle(i);
        }
    }

    /** Open a new empty tab and focus it. */
    public void newTab() {
        addEmptyTab();
        tabs.setSelectedIndex(entries.size() - 1);
    }

    /**
     * Open a file in a tab. Focuses an existing tab if the file is already open.
     * Reuses the current tab if it is empty and untouched; otherwise opens a new tab.
     */
    public void openFileInTab(File f, String content) {
        for (int i = 0; i < entries.size(); i++) {
            if (f.equals(entries.get(i).file)) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
        int cur = tabs.getSelectedIndex();
        if (cur >= 0 && cur < entries.size()) {
            TabEntry e = entries.get(cur);
            if (e.file == null && !e.dirty && e.panel.getText().isBlank()) {
                e.panel.setText(content);
                e.file = f;
                e.dirty = false;
                refreshTitle(cur);
                return;
            }
        }
        EditorPanel panel = buildPanel();
        panel.setText(content);
        TabEntry entry = new TabEntry(panel, f);
        entries.add(entry);
        tabs.addTab(entry.displayName(), panel);
        int idx = entries.size() - 1;
        installTabHeader(idx);
        tabs.setSelectedIndex(idx);
    }

    /** Close the currently active tab. */
    public void closeActiveTab() {
        closeTab(tabs.getSelectedIndex());
    }

    /** Run the currently active panel. */
    public void run() {
        EditorPanel p = getActivePanel();
        if (p != null) p.run();
    }

    public void addTabChangeListener(Runnable r) {
        changeListeners.add(r);
    }

    /** Store and apply a breakpoint listener to all current and future panels. */
    public void setBreakpointListener(BreakpointListener listener) {
        this.breakpointListener = listener;
        for (TabEntry e : entries) {
            LineNumberComponent lnc = e.panel.getLineNumberComponent();
            if (lnc != null) lnc.addBreakpointListener(listener);
        }
    }

    /** Store and apply the before-run hook to all current and future panels. */
    public void setBeforeRunHook(Runnable hook) {
        this.beforeRunHook = hook;
        for (TabEntry e : entries) e.panel.setBeforeRunHook(hook);
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        EditorPanel p = getActivePanel();
        if (p != null) p.onCompileComplete(result);
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private void addEmptyTab() {
        EditorPanel panel = buildPanel();
        TabEntry entry = new TabEntry(panel, null);
        entries.add(entry);
        tabs.addTab(entry.displayName(), panel);
        installTabHeader(entries.size() - 1);
    }

    private EditorPanel buildPanel() {
        EditorPanel panel = new EditorPanel(controller);
        if (beforeRunHook != null) panel.setBeforeRunHook(beforeRunHook);
        if (breakpointListener != null) {
            LineNumberComponent lnc = panel.getLineNumberComponent();
            if (lnc != null) lnc.addBreakpointListener(breakpointListener);
        }
        panel.getEditor().getDocument().addDocumentListener(new DocumentListener() {
            private void markDirty() {
                int i = indexOf(panel);
                if (i >= 0 && entries.get(i).file != null && !entries.get(i).dirty) {
                    entries.get(i).dirty = true;
                    refreshTitle(i);
                }
            }
            public void insertUpdate(DocumentEvent e) { markDirty(); }
            public void removeUpdate(DocumentEvent e) { markDirty(); }
            public void changedUpdate(DocumentEvent e) {}
        });
        return panel;
    }

    private void closeTab(int idx) {
        if (idx < 0 || idx >= entries.size()) return;
        TabEntry entry = entries.get(idx);
        if (entry.dirty) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Save changes to " + (entry.file != null ? entry.file.getName() : "untitled.hd") + "?",
                "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION) return;
        }
        entries.remove(idx);
        tabs.removeTabAt(idx);
        if (entries.isEmpty()) addEmptyTab();
    }

    private void installTabHeader(int idx) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        header.setOpaque(false);
        JLabel title = new JLabel(entries.get(idx).displayName());
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        JLabel close = new JLabel("\u00d7");
        close.setForeground(Color.GRAY);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int i = tabs.indexOfTabComponent(header);
                if (i >= 0) closeTab(i);
            }
            @Override public void mouseEntered(MouseEvent e) { close.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e)  { close.setForeground(Color.GRAY); }
        });
        header.add(title);
        header.add(close);
        header.putClientProperty("lbl", title);
        tabs.setTabComponentAt(idx, header);
    }

    private void refreshTitle(int idx) {
        if (idx < 0 || idx >= entries.size()) return;
        Component c = tabs.getTabComponentAt(idx);
        if (c instanceof JPanel p) {
            Object lbl = p.getClientProperty("lbl");
            if (lbl instanceof JLabel l) l.setText(entries.get(idx).displayName());
        }
    }

    private int indexOf(EditorPanel panel) {
        for (int i = 0; i < entries.size(); i++)
            if (entries.get(i).panel == panel) return i;
        return -1;
    }
}
