package src.gui.output;

import src.gui.core.CompileListener;
import src.gui.core.CompileResult;
import src.gui.core.RuntimeEventListener;
import src.gui.model.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

/**
 * Terminal-styled output panel. Dark background, header shows run time,
 * footer shows completion status.
 */
public class InterpreterOutputPanel extends JPanel implements CompileListener, RuntimeEventListener {
    private static final String PLACEHOLDER = "(Run output will appear here after a successful compilation with no errors.)";
    private static final Pattern INPUT_COMMENT_PATTERN = Pattern.compile("^\\s*//\\s*\\d+\\)\\s*(.+)$");
    private static final Pattern READ_CALL_PATTERN = Pattern.compile("\\bread\\s*\\(");

    private final PanelHeader header;
    private final JTextField inputField;
    private final JButton startButton;
    private final JButton restartButton;
    private final JButton stopButton;
    private final JButton submitInputButton;
    private final JButton loadPresetButton;
    private final JComboBox<String> presetCombo;
    private final JLabel inputStatusLabel;
    private final JLabel expectedInputLabel;
    private final DefaultListModel<String> historyModel;
    private final JList<String> historyList;
    private final JTextArea textArea;
    private final JLabel footerLabel;
    private Consumer<String> onInputSubmitted = null;
    private Runnable onStartRequested = () -> {};
    private Runnable onRestartRequested = () -> {};
    private Runnable onStopRequested = () -> {};
    private boolean runtimeActive;
    private List<String> configuredPrompts = List.of();
    private List<String> queuedPresetLines = new ArrayList<>();
    private final Map<String, List<String>> presetLinesByName = new LinkedHashMap<>();
    private int submittedInputs;

    public InterpreterOutputPanel() {
        super(new BorderLayout());
        header = new PanelHeader();
        header.set("Output", Theme.PANEL_HEADER_NEUTRAL, "");

        inputField = new JTextField();
        inputField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        startButton = new JButton("Start");
        restartButton = new JButton("Restart");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        startButton.addActionListener(e -> onStartRequested.run());
        restartButton.addActionListener(e -> onRestartRequested.run());
        stopButton.addActionListener(e -> onStopRequested.run());

        submitInputButton = new JButton("Send");
        loadPresetButton = new JButton("Load Preset");
        presetCombo = new JComboBox<>();
        presetCombo.setPrototypeDisplayValue("Preset: complex true path");
        configureDefaultPresets();

        inputStatusLabel = new JLabel("Press Enter to send input line");
        inputStatusLabel.setForeground(Theme.LINE_NUMBER_FG);
        expectedInputLabel = new JLabel("Expected input: (none detected)");
        expectedInputLabel.setForeground(Theme.LINE_NUMBER_FG);
        loadPresetButton.addActionListener(e -> {
            String key = (String) presetCombo.getSelectedItem();
            queuedPresetLines = key == null ? new ArrayList<>() : new ArrayList<>(presetLinesByName.getOrDefault(key, List.of()));
            if (queuedPresetLines.isEmpty()) {
                inputStatusLabel.setText("Preset has no input lines");
            } else {
                inputField.setText(queuedPresetLines.get(0));
                inputStatusLabel.setText("Preset loaded: " + key);
                inputField.requestFocusInWindow();
            }
        });

        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setVisibleRowCount(3);
        historyList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = historyList.getSelectedValue();
                    if (selected != null) {
                        inputField.setText(selected);
                        inputField.requestFocusInWindow();
                    }
                }
            }
        });

        submitInputButton.addActionListener(e -> {
            String line = inputField.getText();
            if ((line == null || line.isEmpty()) && !queuedPresetLines.isEmpty()) {
                line = queuedPresetLines.remove(0);
                if (!queuedPresetLines.isEmpty()) {
                    inputField.setText(queuedPresetLines.get(0));
                }
            }
            if (line == null || line.isEmpty()) {
                inputStatusLabel.setText("Type an input value first");
                return;
            }
            inputStatusLabel.setText("Input sent");
            if (onInputSubmitted != null) onInputSubmitted.accept(line);
            rememberInputHistory(line);
            submittedInputs++;
            refreshInputHint();
            inputField.setText("");
            inputField.requestFocusInWindow();
        });
        inputField.addActionListener(e -> submitInputButton.doClick());

        JPanel runControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        runControls.setOpaque(false);
        runControls.add(startButton);
        runControls.add(restartButton);
        runControls.add(stopButton);

        JPanel presetControls = new JPanel(new BorderLayout(6, 0));
        presetControls.setOpaque(false);
        presetControls.add(presetCombo, BorderLayout.CENTER);
        presetControls.add(loadPresetButton, BorderLayout.EAST);

        JPanel inputActions = new JPanel(new BorderLayout(8, 0));
        inputActions.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(submitInputButton);
        left.add(presetControls);
        inputActions.add(left, BorderLayout.WEST);
        inputActions.add(inputStatusLabel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(6, 6));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x3C3C3C)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        JLabel inputLabel = new JLabel("Interactive input (send one line at a time):");
        inputLabel.setForeground(Theme.LINE_NUMBER_FG);
        inputPanel.add(inputLabel, BorderLayout.NORTH);
        JPanel centerInput = new JPanel(new BorderLayout(4, 4));
        centerInput.setOpaque(false);
        JPanel topStack = new JPanel(new BorderLayout(3, 3));
        topStack.setOpaque(false);
        topStack.add(runControls, BorderLayout.NORTH);
        topStack.add(expectedInputLabel, BorderLayout.SOUTH);
        centerInput.add(topStack, BorderLayout.NORTH);
        centerInput.add(inputField, BorderLayout.CENTER);
        JPanel historyPanel = new JPanel(new BorderLayout(3, 3));
        historyPanel.setOpaque(false);
        JLabel historyLabel = new JLabel("Recent Inputs (double-click to reuse):");
        historyLabel.setForeground(Theme.LINE_NUMBER_FG);
        historyPanel.add(historyLabel, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyList), BorderLayout.CENTER);
        centerInput.add(historyPanel, BorderLayout.SOUTH);
        inputPanel.add(centerInput, BorderLayout.CENTER);
        inputPanel.add(inputActions, BorderLayout.SOUTH);

        textArea = new JTextArea(PLACEHOLDER);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setMargin(new Insets(6, 8, 6, 8));
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        footerLabel = new JLabel(" ");
        footerLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        footerLabel.setForeground(Theme.LINE_NUMBER_FG);
        footerLabel.setBackground(Theme.PANEL_HEADER_BG);
        footerLabel.setOpaque(true);
        footerLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x3C3C3C)),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)
        ));

        add(header, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.add(inputPanel, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(footerLabel, BorderLayout.SOUTH);
    }

    public void setOnInputSubmitted(Consumer<String> onInputSubmitted) {
        this.onInputSubmitted = onInputSubmitted;
    }

    public void setOnStartRequested(Runnable onStartRequested) {
        this.onStartRequested = onStartRequested != null ? onStartRequested : () -> {};
    }

    public void setOnRestartRequested(Runnable onRestartRequested) {
        this.onRestartRequested = onRestartRequested != null ? onRestartRequested : () -> {};
    }

    public void setOnStopRequested(Runnable onStopRequested) {
        this.onStopRequested = onStopRequested != null ? onStopRequested : () -> {};
    }

    private void configureDefaultPresets() {
        presetLinesByName.clear();
        presetLinesByName.put("No preset", List.of());
        presetLinesByName.put("Complex: true path", List.of("5", "3", "true", "\"demo\""));
        presetLinesByName.put("Complex: false path", List.of("5", "3", "false", "\"demo\""));
        presetLinesByName.put("Complex: clamp n", List.of("30", "2", "true", "\"cap\""));
        presetCombo.removeAllItems();
        for (String key : presetLinesByName.keySet()) {
            presetCombo.addItem(key);
        }
        presetCombo.setSelectedIndex(0);
    }

    private void rememberInputHistory(String line) {
        if (line == null || line.isBlank()) return;
        for (int i = 0; i < historyModel.getSize(); i++) {
            if (line.equals(historyModel.getElementAt(i))) {
                historyModel.remove(i);
                break;
            }
        }
        historyModel.add(0, line);
        while (historyModel.size() > 12) {
            historyModel.remove(historyModel.size() - 1);
        }
    }

    public void configureInputPrompts(String sourceText) {
        submittedInputs = 0;
        queuedPresetLines = new ArrayList<>();
        configuredPrompts = extractConfiguredPrompts(sourceText);
        refreshInputHint();
    }

    private List<String> extractConfiguredPrompts(String sourceText) {
        if (sourceText == null || sourceText.isBlank()) return List.of();
        List<String> prompts = new ArrayList<>();
        String[] lines = sourceText.split("\\r?\\n", -1);
        for (String line : lines) {
            Matcher m = INPUT_COMMENT_PATTERN.matcher(line);
            if (m.matches()) prompts.add(m.group(1).trim());
        }
        if (!prompts.isEmpty()) return prompts;

        Matcher readMatcher = READ_CALL_PATTERN.matcher(sourceText);
        int reads = 0;
        while (readMatcher.find()) reads++;
        for (int i = 1; i <= reads; i++) {
            prompts.add("Input #" + i);
        }
        return prompts;
    }

    private void refreshInputHint() {
        if (configuredPrompts.isEmpty()) {
            expectedInputLabel.setText("Expected input: (none detected from comments/read calls)");
            return;
        }
        int nextIndex = Math.min(submittedInputs, configuredPrompts.size() - 1);
        if (submittedInputs >= configuredPrompts.size()) {
            expectedInputLabel.setText("Expected input: complete (" + configuredPrompts.size() + "/" + configuredPrompts.size() + ")");
        } else {
            expectedInputLabel.setText("Expected input " + (submittedInputs + 1) + "/" + configuredPrompts.size() + ": " + configuredPrompts.get(nextIndex));
        }
    }

    @Override
    public void onCompileComplete(CompileResult result) {
        if (runtimeActive) return;
        if (result == null || result.interpreterOutput().isEmpty()) {
            header.set("Output", Theme.PANEL_HEADER_NEUTRAL, "");
            textArea.setText(PLACEHOLDER);
            footerLabel.setText(" ");
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
            restartButton.setEnabled(true);
            textArea.setCaretPosition(0);
            return;
        }
        String output = result.interpreterOutput().get();
        String timeStr = PanelHeader.ms(result.metrics().runTimeNs);
        header.set("Output", Theme.PANEL_HEADER_OK, timeStr);
        if (output.isBlank()) {
            textArea.setText("");
            footerLabel.setText("[Program produced no output]");
        } else {
            textArea.setText(output);
            footerLabel.setText("[Program completed  " + timeStr + "]");
        }
        stopButton.setEnabled(false);
        startButton.setEnabled(true);
        restartButton.setEnabled(true);
        textArea.setCaretPosition(0);
    }

    @Override
    public void onRuntimeStarted() {
        SwingUtilities.invokeLater(() -> {
            runtimeActive = true;
            textArea.setText("");
            footerLabel.setText("[Program running - submit one input per line]");
            header.set("Output", Theme.PANEL_HEADER_NEUTRAL, "");
            inputStatusLabel.setText("Program started - waiting for input");
            stopButton.setEnabled(true);
            startButton.setEnabled(false);
            restartButton.setEnabled(false);
            inputField.requestFocusInWindow();
        });
    }

    @Override
    public void onRuntimeOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    @Override
    public void onRuntimeFinished(String finalOutput, String runtimeError) {
        SwingUtilities.invokeLater(() -> {
            runtimeActive = false;
            if (runtimeError != null && !runtimeError.isBlank()) {
                if (!textArea.getText().isEmpty() && !textArea.getText().endsWith(System.lineSeparator())) {
                    textArea.append(System.lineSeparator());
                }
                textArea.append("Runtime error: " + runtimeError + System.lineSeparator());
                footerLabel.setText("[Program failed]");
                inputStatusLabel.setText("Run failed");
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                restartButton.setEnabled(true);
                return;
            }
            footerLabel.setText("[Program completed]");
            inputStatusLabel.setText("Run finished");
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
            restartButton.setEnabled(true);
        });
    }
}
