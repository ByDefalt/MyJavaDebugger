package gui;

import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;
import gui.components.CallStackPanel;
import gui.components.OutputPanel;
import gui.components.SourceCodePanel;
import gui.components.ToolbarPanel;
import gui.components.VariablesPanel;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import models.DebugFrame;
import models.DebuggerState;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Interface graphique principale du debugger refactorisée (SRP + Composition)
 *
 * Architecture :
 * - Utilise des composants réutilisables (ToolbarPanel, SourceCodePanel, etc.)
 * - Délègue les responsabilités via des interfaces (DebuggerController)
 * - Thème configurable via ThemeManager
 */
public class ModernDebuggerGUI extends JFrame {

    // Composants UI
    private final ToolbarPanel toolbar;
    private final SourceCodePanel sourceCodePanel;
    private final CallStackPanel callStackPanel;
    private final VariablesPanel variablesPanel;
    private final OutputPanel outputPanel;

    // État
    private DebuggerState state;
    private String currentSourceFile = "";
    private DebuggerController controller;

    private final Theme theme;

    /**
     * Interface pour le contrôleur du debugger (DIP - Dependency Inversion)
     */
    public interface DebuggerController {
        void onContinue() throws Exception;
        void onStepOver() throws Exception;
        void onStepInto() throws Exception;
        void onStop();
        void onBreakpointToggle(String file, int line) throws Exception;
    }

    public ModernDebuggerGUI() {
        super("Java Debugger Pro");
        this.theme = ThemeManager.getInstance().getTheme();

        // Initialiser les composants
        this.toolbar = new ToolbarPanel();
        this.sourceCodePanel = new SourceCodePanel();
        this.callStackPanel = new CallStackPanel();
        this.variablesPanel = new VariablesPanel();
        this.outputPanel = new OutputPanel();

        initFrame();
        initLayout();
        initListeners();
    }

    private void initFrame() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setSize(1400, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initLayout() {
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setBackground(theme.getBackgroundSecondary());

        // Zone droite : Stack + Variables
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                callStackPanel, variablesPanel);
        rightSplit.setDividerLocation(250);
        rightSplit.setBorder(null);
        rightSplit.setBackground(theme.getBackgroundSecondary());

        // Zone principale : Code + Panneaux droits
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                sourceCodePanel, rightSplit);
        mainSplit.setDividerLocation(900);
        mainSplit.setBorder(null);

        // Zone complète : Principal + Console
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                mainSplit, outputPanel);
        bottomSplit.setDividerLocation(600);
        bottomSplit.setBorder(null);

        mainContainer.add(toolbar, BorderLayout.NORTH);
        mainContainer.add(bottomSplit, BorderLayout.CENTER);

        add(mainContainer);
    }

    private void initListeners() {
        // Toolbar
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
            public void onStop() {
                if (controller != null) controller.onStop();
            }
        });

        // Source code breakpoints
        sourceCodePanel.setBreakpointListener(line -> {
            if (controller != null && !currentSourceFile.isEmpty()) {
                controller.onBreakpointToggle(currentSourceFile, line);
            }
        });

        // Call stack navigation
        callStackPanel.setCallStackListener(this::onStackFrameSelected);
    }

    private void executeControllerAction(ControllerAction action) {
        if (controller != null) {
            try {
                action.execute();
            } catch (Exception e) {
                appendOutput("Error: " + e.getMessage() + "\n");
            }
        }
    }

    @FunctionalInterface
    private interface ControllerAction {
        void execute() throws Exception;
    }

    /**
     * Gère la sélection d'un frame dans la pile d'appels
     */
    private void onStackFrameSelected(int index) {
        if (state == null || state.getContext() == null) return;

        try {
            List<DebugFrame> frames = state.getContext().getCallStack().getFrames();
            if (index < frames.size()) {
                DebugFrame selectedFrame = frames.get(index);

                // Mettre à jour les variables
                variablesPanel.updateVariables(selectedFrame.getTemporaries());

                // Mettre à jour le code source
                Location loc = selectedFrame.getLocation();
                if (loc != null) {
                    currentSourceFile = loc.sourceName();
                    loadSource(loc);
                    sourceCodePanel.setCurrentLine(loc.lineNumber());
                }
            }
        } catch (Exception e) {
            appendOutput("Error navigating stack: " + e.getMessage() + "\n");
        }
    }

    // ========== API Publique ==========

    public void setController(DebuggerController controller) {
        this.controller = controller;
    }

    /**
     * Met à jour l'état complet du debugger
     */
    public void updateDebuggerState(DebuggerState state, Location loc, ThreadReference thread) {
        this.state = state;
        if (loc != null) {
            try {
                currentSourceFile = loc.sourceName();
                loadSource(loc);

                // Mettre à jour la pile
                if (state.getContext() != null) {
                    callStackPanel.updateStack(state.getContext().getCallStack().getFrames());
                    callStackPanel.selectFrame(0);
                }

                // Mettre à jour les variables du premier frame
                if (state.getContext() != null && !state.getContext().getCallStack().getFrames().isEmpty()) {
                    variablesPanel.updateVariables(
                            state.getContext().getCallStack().getFrames().get(0).getTemporaries());
                }
            } catch (Exception e) {
                appendOutput("Error: " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Charge le code source depuis un fichier
     */
    private void loadSource(Location loc) throws Exception {
        String sourceName = loc.sourceName();
        String className = loc.declaringType().name();
        String packagePath = className.contains(".")
                ? className.substring(0, className.lastIndexOf('.')).replace('.', '/') + "/"
                : "";

        // Chercher dans plusieurs emplacements possibles
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
                appendOutput("[LOADED] Source: " + path + "\n");
                return;
            }
        }

        appendOutput("[WARN] Source file not found: " + sourceName + " (tried " + possiblePaths.length + " paths)\n");
    }

    /**
     * Ajoute du texte à la console
     */
    public void appendOutput(String text) {
        outputPanel.appendOutput(text);
    }

    /**
     * Active/désactive les contrôles
     */
    public void setControlsEnabled(boolean enabled) {
        toolbar.setControlsEnabled(enabled);
    }

    /**
     * Retourne le panel de code source
     */
    public SourceCodePanel getSourceCodePanel() {
        return sourceCodePanel;
    }
}
