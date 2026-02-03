package gui;
import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;
import gui.components.CallStackPanel;
import gui.components.MethodCallsPanel;
import gui.components.OutputPanel;
import gui.components.SourceCodePanel;
import gui.components.ToolbarPanel;
import gui.components.VariableHistoryPanel;
import gui.components.VariablesPanel;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import models.DebugFrame;
import models.DebuggerState;
import models.ExecutionSnapshot;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
public class DebuggerGUI extends JFrame {
    private final ToolbarPanel toolbar;
    private final SourceCodePanel sourceCodePanel;
    private final CallStackPanel callStackPanel;
    private final VariablesPanel variablesPanel;
    private final OutputPanel outputPanel;
    private final OutputPanel debugLogPanel;
    private final VariableHistoryPanel variableHistoryPanel;
    private final MethodCallsPanel methodCallsPanel;
    private DebuggerState state;
    private String currentSourceFile = "";
    private DebuggerController controller;
    private List<ExecutionSnapshot> executionSnapshots = new ArrayList<>();
    private List<DebugFrame> currentFrames = new ArrayList<>();
    private ExecutionSnapshot currentSnapshot;
    private final Theme theme;
    public interface DebuggerController {
        void onContinue() throws Exception;
        void onStepOver() throws Exception;
        void onStepInto() throws Exception;
        void onStepBack() throws Exception;
        void onStop();
        void onBreakpointToggle(String file, int line) throws Exception;
        void onNavigateToStep(int stepNumber);
    }
    public DebuggerGUI() {
        super("Java Debugger Pro");
        this.theme = ThemeManager.getInstance().getTheme();
        this.toolbar = new ToolbarPanel();
        this.sourceCodePanel = new SourceCodePanel();
        this.callStackPanel = new CallStackPanel();
        this.variablesPanel = new VariablesPanel();
        this.outputPanel = new OutputPanel("Program Output");
        this.debugLogPanel = new OutputPanel("Debugger Log");
        this.variableHistoryPanel = new VariableHistoryPanel();
        this.methodCallsPanel = new MethodCallsPanel();
        initFrame();
        initLayout();
        initListeners();
    }
    private void initFrame() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.arc", 6);
            UIManager.put("Component.arc", 6);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("ScrollBar.thumbArc", 6);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        } catch (Exception ignored) {}
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(theme.getBackgroundPrimary());
    }
    private void initLayout() {
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setBackground(theme.getBackgroundPrimary());
        JPanel variablesWithHistory = new JPanel(new BorderLayout());
        variablesWithHistory.setBackground(theme.getBackgroundPrimary());
        variablesWithHistory.add(variablesPanel, BorderLayout.CENTER);
        variablesWithHistory.add(variableHistoryPanel, BorderLayout.SOUTH);
        variableHistoryPanel.setPreferredSize(new Dimension(0, 200));
        variableHistoryPanel.setVisible(false);
        JPanel sourceWithMethodCalls = new JPanel(new BorderLayout());
        sourceWithMethodCalls.setBackground(theme.getBackgroundPrimary());
        sourceWithMethodCalls.add(sourceCodePanel, BorderLayout.CENTER);
        methodCallsPanel.setPreferredSize(new Dimension(350, 0));
        methodCallsPanel.setVisible(false);
        sourceWithMethodCalls.add(methodCallsPanel, BorderLayout.WEST);
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                callStackPanel, variablesWithHistory);
        rightSplit.setDividerLocation(250);
        rightSplit.setDividerSize(1);
        rightSplit.setBorder(null);
        rightSplit.setBackground(theme.getBorderColor());
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                sourceWithMethodCalls, rightSplit);
        mainSplit.setDividerLocation(900);
        mainSplit.setDividerSize(1);
        mainSplit.setBorder(null);
        mainSplit.setBackground(theme.getBorderColor());

        JSplitPane consoleSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                outputPanel, debugLogPanel);
        consoleSplit.setDividerLocation(700);
        consoleSplit.setDividerSize(1);
        consoleSplit.setBorder(null);
        consoleSplit.setBackground(theme.getBorderColor());
        consoleSplit.setResizeWeight(0.5);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                mainSplit, consoleSplit);
        bottomSplit.setDividerLocation(600);
        bottomSplit.setDividerSize(1);
        bottomSplit.setBorder(null);
        bottomSplit.setBackground(theme.getBorderColor());
        mainContainer.add(toolbar, BorderLayout.NORTH);
        mainContainer.add(bottomSplit, BorderLayout.CENTER);
        add(mainContainer);
    }
    private void initListeners() {
        toolbar.setToolbarListener(new ToolbarPanel.ToolbarListener() {
            @Override
            public void onContinue() {
                executeControllerAction(() -> controller.onContinue());
            }
            @Override
            public void onStepOver() {
                executeControllerAction(() -> controller.onStepOver());
            }
            @Override
            public void onStepInto() {
                executeControllerAction(() -> controller.onStepInto());
            }
            @Override
            public void onStepBack() {
                executeControllerAction(() -> controller.onStepBack());
            }
            @Override
            public void onStop() {
                if (controller != null) controller.onStop();
            }
            @Override
            public void onShowMethodCalls() {
                showMethodCallsPanel();
            }
        });
        sourceCodePanel.setBreakpointListener(line -> {
            if (controller != null && !currentSourceFile.isEmpty()) {
                controller.onBreakpointToggle(currentSourceFile, line);
            }
        });
        callStackPanel.setCallStackListener(this::onStackFrameSelected);
        callStackPanel.setFindCallsListener(this::showMethodCallsForMethod);
        variablesPanel.setSelectionListener(this::showVariableHistory);
        variableHistoryPanel.setListener(new VariableHistoryPanel.HistoryPanelListener() {
            @Override
            public void onClose() {
                variableHistoryPanel.setVisible(false);
                revalidate();
                repaint();
            }
            @Override
            public void onStepSelected(int stepNumber) {
                navigateToStep(stepNumber);
            }
        });
        methodCallsPanel.setListener(new MethodCallsPanel.MethodCallsListener() {
            @Override
            public void onMethodCallSelected(int stepNumber) {
                navigateToStep(stepNumber);
            }
            @Override
            public void onClose() {
                methodCallsPanel.setVisible(false);
                revalidate();
                repaint();
            }
        });
    }
    private void executeControllerAction(ControllerAction action) {
        if (controller != null) {
            try {
                action.execute();
            } catch (Exception e) {
                appendDebugLog("Error: " + e.getMessage() + "\n");
            }
        }
    }
    @FunctionalInterface
    private interface ControllerAction {
        void execute() throws Exception;
    }
    private void onStackFrameSelected(int index) {
        try {
            if (currentSnapshot != null && !currentFrames.isEmpty()) {
                if (index < currentFrames.size()) {
                    DebugFrame selectedFrame = currentFrames.get(index);
                    variablesPanel.updateFromSnapshots(currentSnapshot.getVariablesForFrame(index));
                    String sourceFile = selectedFrame.getSourceFile();
                    int lineNumber = selectedFrame.getLineNumber();
                    if (sourceFile != null) {
                        currentSourceFile = sourceFile;
                        loadSourceFromFrame(selectedFrame);
                        sourceCodePanel.setCurrentLine(lineNumber);
                    }
                }
                return;
            }
            if (state == null || state.getContext() == null) return;
            List<DebugFrame> frames = state.getContext().getCallStack().getFrames();
            if (index < frames.size()) {
                DebugFrame selectedFrame = frames.get(index);
                variablesPanel.updateVariables(selectedFrame.getTemporaries());
                Location loc = selectedFrame.getLocation();
                if (loc != null) {
                    currentSourceFile = loc.sourceName();
                    loadSource(loc);
                    sourceCodePanel.setCurrentLine(loc.lineNumber());
                }
            }
        } catch (Exception e) {
            appendDebugLog("Error navigating stack: " + e.getMessage() + "\n");
        }
    }
    private void loadSourceFromFrame(DebugFrame frame) {
        String sourceName = frame.getSourceFile();
        String displayName = frame.getDisplayName();
        String packagePath = "";
        if (displayName != null && displayName.contains(".")) {
            String className = displayName.substring(0, displayName.lastIndexOf('.'));
            if (className.contains(".")) {
                packagePath = className.substring(0, className.lastIndexOf('.')).replace('.', '/') + "/";
            }
        }
        String[] possiblePaths = {
                "src/main/java/" + packagePath + sourceName,
                "src/main/java/dbg/" + sourceName,
                "src/main/java/" + sourceName,
                "src/" + packagePath + sourceName,
                sourceName
        };
        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                try {
                    List<String> lines = Files.readAllLines(Paths.get(path));
                    sourceCodePanel.setSourceLines(lines);
                    return;
                } catch (Exception e) {
                    appendDebugLog("[WARN] Error loading source: " + e.getMessage() + "\n");
                }
            }
        }
    }
    public void setController(DebuggerController controller) {
        this.controller = controller;
    }
    public void updateDebuggerState(DebuggerState state, Location loc, ThreadReference thread) {
        this.state = state;
        this.currentSnapshot = null;
        this.currentFrames.clear();
        if (loc != null) {
            try {
                currentSourceFile = loc.sourceName();
                loadSource(loc);
                if (state.getContext() != null) {
                    callStackPanel.updateStack(state.getContext().getCallStack().getFrames());
                    callStackPanel.selectFrame(0);
                }
                if (state.getContext() != null && !state.getContext().getCallStack().getFrames().isEmpty()) {
                    variablesPanel.updateVariables(
                            state.getContext().getCallStack().getFrames().get(0).getTemporaries());
                }
            } catch (Exception e) {
                appendDebugLog("Error: " + e.getMessage() + "\n");
            }
        }
    }
    private void loadSource(Location loc) throws Exception {
        String sourceName = loc.sourceName();
        String className = loc.declaringType().name();
        String packagePath = className.contains(".")
                ? className.substring(0, className.lastIndexOf('.')).replace('.', '/') + "/"
                : "";
        String[] possiblePaths = {
                "src/main/java/" + packagePath + sourceName,
                "src/main/java/dbg/" + sourceName,
                "src/main/java/" + sourceName,
                "src/" + packagePath + sourceName,
                sourceName
        };
        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                List<String> lines = Files.readAllLines(Paths.get(path));
                sourceCodePanel.setSourceLines(lines);
                sourceCodePanel.setCurrentLine(loc.lineNumber());
                appendDebugLog("[LOADED] Source: " + path + "\n");
                return;
            }
        }
        appendDebugLog("[WARN] Source file not found: " + sourceName + " (tried " + possiblePaths.length + " paths)\n");
    }
    public void appendOutput(String text) {
        outputPanel.appendOutput(text);
    }

    public void appendDebugLog(String text) {
        debugLogPanel.appendOutput(text);
    }

    public void clearOutput() {
        outputPanel.clear();
    }

    public void clearDebugLog() {
        debugLogPanel.clear();
    }

    public void setControlsEnabled(boolean enabled) {
        toolbar.setControlsEnabled(enabled);
    }
    public SourceCodePanel getSourceCodePanel() {
        return sourceCodePanel;
    }
    public void updateFromSnapshot(ExecutionSnapshot snapshot) {
        if (snapshot == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                this.currentSnapshot = snapshot;
                currentSourceFile = snapshot.getSourceFile();
                loadSourceFromSnapshot(snapshot);
                this.currentFrames = convertStackFrames(snapshot.getStackFrames());
                callStackPanel.updateStack(currentFrames);
                if (!currentFrames.isEmpty()) {
                    callStackPanel.selectFrame(0);
                }
                variablesPanel.updateFromSnapshots(snapshot.getVariablesForFrame(0));

                appendDebugLog(String.format("[TIME TRAVEL] Step #%d: %s:%d - %s.%s()\n",
                        snapshot.getStepNumber(),
                        snapshot.getSourceFile(),
                        snapshot.getLineNumber(),
                        snapshot.getClassName(),
                        snapshot.getMethodName()));
            } catch (Exception e) {
                appendDebugLog("[ERROR] Time travel failed: " + e.getMessage() + "\n");
            }
        });
    }
    private void loadSourceFromSnapshot(ExecutionSnapshot snapshot) {
        loadSourceFromSnapshot(snapshot, snapshot.getLineNumber());
    }
    private void loadSourceFromSnapshot(ExecutionSnapshot snapshot, int lineNumber) {
        String sourceName = snapshot.getSourceFile();
        String className = snapshot.getClassName();
        String packagePath = className.contains(".")
                ? className.substring(0, className.lastIndexOf('.')).replace('.', '/') + "/"
                : "";
        String[] possiblePaths = {
                "src/main/java/" + packagePath + sourceName,
                "src/main/java/dbg/" + sourceName,
                "src/main/java/" + sourceName,
                "src/" + packagePath + sourceName,
                sourceName
        };
        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                try {
                    List<String> lines = Files.readAllLines(Paths.get(path));
                    sourceCodePanel.setSourceLines(lines);
                    SwingUtilities.invokeLater(() -> sourceCodePanel.setCurrentLine(lineNumber));
                    return;
                } catch (Exception e) {
                    appendDebugLog("[WARN] Error loading source: " + e.getMessage() + "\n");
                }
            }
        }
    }
    private List<DebugFrame> convertStackFrames(List<ExecutionSnapshot.StackFrameSnapshot> stackFrames) {
        List<DebugFrame> frames = new ArrayList<>();
        for (ExecutionSnapshot.StackFrameSnapshot sf : stackFrames) {
            DebugFrame frame = new DebugFrame(
                    sf.getClassName() + "." + sf.getMethodName() + "()",
                    sf.getSourceFile(),
                    sf.getLineNumber()
            );
            frames.add(frame);
        }
        return frames;
    }
    public void setExecutionSnapshots(List<ExecutionSnapshot> snapshots) {
        this.executionSnapshots = snapshots != null ? snapshots : new ArrayList<>();
    }
    private void showVariableHistory(String variableId, String variableName) {
        if (executionSnapshots.isEmpty()) {
            appendDebugLog("[INFO] No execution history available. Run with recording enabled.\n");
            return;
        }
        variableHistoryPanel.showVariableHistory(variableId, variableName, executionSnapshots);
        variableHistoryPanel.setVisible(true);
        revalidate();
        repaint();
        appendDebugLog("[HISTORY] Showing history for variable: " + variableName + "\n");
    }
    private void navigateToStep(int stepNumber) {
        for (int i = 0; i < executionSnapshots.size(); i++) {
            ExecutionSnapshot snapshot = executionSnapshots.get(i);
            if (snapshot.getStepNumber() == stepNumber) {
                updateFromSnapshot(snapshot);
                if (controller != null) {
                    controller.onNavigateToStep(i);
                }
                appendDebugLog("[TIME TRAVEL] Navigated to step #" + stepNumber + "\n");
                return;
            }
        }
        appendDebugLog("[ERROR] Step #" + stepNumber + " not found\n");
    }
    public VariableHistoryPanel getVariableHistoryPanel() {
        return variableHistoryPanel;
    }
    public VariablesPanel getVariablesPanel() {
        return variablesPanel;
    }
    private void showMethodCallsPanel() {
        if (executionSnapshots.isEmpty()) {
            appendDebugLog("[INFO] No execution history available. Run with recording enabled.\n");
            return;
        }
        methodCallsPanel.updateMethodCalls(executionSnapshots);
        methodCallsPanel.setTitle("METHOD CALLS");
        methodCallsPanel.setVisible(true);
        revalidate();
        repaint();
        appendDebugLog("[METHOD CALLS] Showing " + executionSnapshots.size() + " recorded method calls\n");
    }
    private void showMethodCallsForMethod(String className, String methodName) {
        if (executionSnapshots.isEmpty()) {
            appendDebugLog("[INFO] No execution history available. Run with recording enabled.\n");
            return;
        }
        methodCallsPanel.updateMethodCalls(executionSnapshots);
        methodCallsPanel.filterByClassAndMethod(className, methodName);
        String shortClassName = className.contains(".") ?
                className.substring(className.lastIndexOf('.') + 1) : className;
        methodCallsPanel.setTitle("CALLS TO " + shortClassName + "." + methodName + "()");
        methodCallsPanel.setVisible(true);
        revalidate();
        repaint();
        appendDebugLog("[FIND CALLS] Searching calls to " + shortClassName + "." + methodName + "()\n");
    }
    public MethodCallsPanel getMethodCallsPanel() {
        return methodCallsPanel;
    }
}
