package gui;

import commands.*;
import models.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class DebuggerGUI extends JFrame {
    private SourceCodePanel sourceCodePanel;
    private JList<String> callStackList;
    private JTree inspectorTree;
    private JTextArea outputArea;
    private JButton stepOverButton, stepIntoButton, continueButton, stopButton;

    private DefaultListModel<String> callStackModel;
    private DefaultTreeModel inspectorTreeModel;
    private DefaultMutableTreeNode inspectorRoot;

    private DebuggerState state;
    private String currentSourceFile = "";
    private DebuggerCallback callback;

    public interface DebuggerCallback {
        void executeCommand(Command command) throws Exception;
        void placeBreakpoint(String file, int line) throws Exception;
        void stop();
    }

    public DebuggerGUI() throws Exception {
        super("Java Debugger Pro");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        initComponents();
        setSize(1280, 850);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initComponents() throws Exception {
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setBackground(new Color(37, 37, 38));

        // ToolBar stylisée
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        toolBar.setBackground(new Color(45, 45, 48));
        toolBar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(60, 60, 60)));

        stepIntoButton = createStyledButton("Step Into", new Color(0, 122, 204));
        stepOverButton = createStyledButton("Step Over", new Color(0, 122, 204));
        continueButton = createStyledButton("Continue", new Color(30, 150, 70));
        stopButton = createStyledButton("Stop", new Color(200, 50, 50));

        stepIntoButton.addActionListener(e -> { if(callback!=null) {
            try {
                callback.executeCommand(new StepCommand());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        });
        stepOverButton.addActionListener(e -> { if(callback!=null) {
            try {
                callback.executeCommand(new StepOverCommand());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        });
        continueButton.addActionListener(e -> { if(callback!=null) {
            try {
                callback.executeCommand(new ContinueCommand());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        });
        stopButton.addActionListener(e -> {
            System.exit(0);
        });

        toolBar.add(continueButton);
        toolBar.add(new JSeparator(JSeparator.VERTICAL));
        toolBar.add(stepOverButton);
        toolBar.add(stepIntoButton);
        toolBar.add(Box.createHorizontalStrut(20));
        toolBar.add(stopButton);

        // Split Panels
        sourceCodePanel = new SourceCodePanel();
        sourceCodePanel.setBreakpointListener(line -> {
            if (callback != null && !currentSourceFile.isEmpty()) callback.placeBreakpoint(currentSourceFile, line);
        });

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createStackPanel(), createInspectorPanel());
        rightSplit.setDividerLocation(300);
        rightSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourceCodePanel, rightSplit);
        mainSplit.setDividerLocation(850);
        mainSplit.setBorder(null);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, createOutputPanel());
        bottomSplit.setDividerLocation(600);
        bottomSplit.setBorder(null);

        mainContainer.add(toolBar, BorderLayout.NORTH);
        mainContainer.add(bottomSplit, BorderLayout.CENTER);
        add(mainContainer);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel createStackPanel() {
        callStackModel = new DefaultListModel<>();
        callStackList = new JList<>(callStackModel);
        callStackList.setBackground(new Color(30, 30, 30));
        callStackList.setForeground(new Color(200, 200, 200));
        return wrapInPanel(new JScrollPane(callStackList), "CALL STACK");
    }

    private JPanel createInspectorPanel() {
        inspectorRoot = new DefaultMutableTreeNode("Variables");
        inspectorTreeModel = new DefaultTreeModel(inspectorRoot);
        inspectorTree = new JTree(inspectorTreeModel);
        inspectorTree.setBackground(new Color(30, 30, 30));
        inspectorTree.setForeground(new Color(200, 200, 200));
        return wrapInPanel(new JScrollPane(inspectorTree), "VARIABLES");
    }

    private JPanel createOutputPanel() {
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(30, 30, 30));
        outputArea.setForeground(new Color(150, 250, 150));
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        return wrapInPanel(new JScrollPane(outputArea), "DEBUG CONSOLE");
    }

    private JPanel wrapInPanel(JComponent comp, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(37, 37, 38));
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 60)), title);
        border.setTitleColor(new Color(150, 150, 150));
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 10));
        p.setBorder(border);
        p.add(comp);
        return p;
    }

    public void updateDebuggerState(DebuggerState state, Location loc, ThreadReference thread) {
        this.state = state;
        if (loc != null) {
            try {
                currentSourceFile = loc.sourceName();
                loadSource(loc);
                updateStack();
                updateInspector(0);
            } catch (Exception e) { appendOutput("Error: " + e.getMessage() + "\n"); }
        }
    }

    private void loadSource(Location loc) throws Exception {
        String path = "src/main/java/dbg/" + loc.sourceName();
        if (Files.exists(Paths.get(path))) {
            List<String> lines = Files.readAllLines(Paths.get(path));
            sourceCodePanel.setSourceLines(lines);
            sourceCodePanel.setCurrentLine(loc.lineNumber());
        }
    }

    private void updateStack() {
        callStackModel.clear();
        if (state != null && state.getContext() != null) {
            state.getContext().getCallStack().getFrames().forEach(f -> callStackModel.addElement(f.toString()));
        }
    }

    private void updateInspector(int frameIdx) {
        inspectorRoot.removeAllChildren();
        if (state != null && state.getContext() != null) {
            List<DebugFrame> frames = state.getContext().getCallStack().getFrames();
            if (frameIdx < frames.size()) {
                frames.get(frameIdx).getTemporaries().forEach(v ->
                        inspectorRoot.add(new DefaultMutableTreeNode(v.getName() + " = " + v.getValueAsString())));
            }
        }
        inspectorTreeModel.reload();
    }

    public void appendOutput(String txt) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(txt);
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    public void setCallback(DebuggerCallback cb) { this.callback = cb; }
    public void enableControls(boolean b) {
        stepIntoButton.setEnabled(b);
        stepOverButton.setEnabled(b);
        continueButton.setEnabled(b);
    }
}