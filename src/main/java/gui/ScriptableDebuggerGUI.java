package gui;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import commands.*;
import dbg.AbstractDebugger;
import io.GUILogger;
import io.Logger;

import javax.swing.*;
import java.util.*;

/**
 * Contrôleur du debugger avec GUI
 *
 * Hérite de AbstractDebugger (dbg) pour le code commun
 * Implémente DebuggerGUI.DebuggerController pour l'interface graphique
 */
public class ScriptableDebuggerGUI extends AbstractDebugger
        implements DebuggerGUI.DebuggerController {

    private DebuggerGUI gui;
    private Logger log;
    private volatile boolean guiReady = false;

    // ========== Implémentation des méthodes abstraites ==========

    @Override
    protected void initializeUI() {
        SwingUtilities.invokeLater(() -> {
            gui = new DebuggerGUI();
            gui.setController(this);
            gui.setVisible(true);

            // Créer le logger GUI (DIP - injection de la dépendance)
            log = new GUILogger(gui::appendOutput, Logger.Level.DEBUG);

            guiReady = true;
        });

        // Attendre que la GUI soit prête
        while (!guiReady) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    protected void onBeforeStart() {
        // Rien de spécial
    }

    @Override
    protected void onInfo(String message) {
        if (log != null) {
            log.info(message);
        }
    }

    @Override
    protected void onError(String message) {
        if (log != null) {
            log.error(message);
        }
    }

    @Override
    protected void onOutput(String output) {
        // Output du programme debuggé - pas de préfixe
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.appendOutput(output));
        }
    }

    @Override
    protected void onVMDisconnect() {
        if (log != null) {
            log.info("Process finished.");
        }
        SwingUtilities.invokeLater(() -> gui.setControlsEnabled(false));
    }

    @Override
    protected void onBreakpoint(Location loc, ThreadReference thread) throws Exception {
        if (log != null) {
            log.info("Breakpoint at %s:%d", loc.sourceName(), loc.lineNumber());
        }
        updateUIAndWait(loc, thread);
    }

    @Override
    protected void onStep(Location loc, ThreadReference thread) throws Exception {
        if (log != null) {
            log.debug("Step at %s:%d", loc.sourceName(), loc.lineNumber());
        }
        updateUIAndWait(loc, thread);
    }

    @Override
    protected void onClassPrepare(ReferenceType refType) {
        if (log != null) {
            log.debug("Class loaded: %s", refType.name());
        }
        setInitialBreakpoint();
    }

    // ========== Méthodes spécifiques ==========

    private void setInitialBreakpoint() {
        if (log != null) {
            log.debug("Looking for class: %s", debugClass.getName());
            log.debug("All classes count: %d", vm.allClasses().size());
        }

        for (ReferenceType type : vm.allClasses()) {
            if (type.name().equals(debugClass.getName())) {
                if (log != null) {
                    log.debug("Found class: %s", type.name());
                }
                try {
                    for (int lineNum = 13; lineNum <= 25; lineNum++) {
                        List<Location> locs = type.locationsOfLine(lineNum);
                        if (!locs.isEmpty()) {
                            vm.eventRequestManager().createBreakpointRequest(locs.get(0)).enable();
                            if (log != null) {
                                log.info("Breakpoint set at line %d", lineNum);
                            }
                            return;
                        }
                    }
                    if (log != null) {
                        log.warn("No executable lines found in range 13-25");
                    }
                } catch (Exception e) {
                    if (log != null) {
                        log.error("Error setting breakpoint", e);
                    }
                }
            }
        }
        if (log != null) {
            log.warn("Class not found in VM classes");
        }
    }

    private void updateUIAndWait(Location loc, ThreadReference thread) throws Exception {
        state.updateContext(thread);
        SwingUtilities.invokeLater(() -> {
            gui.updateDebuggerState(state, loc, thread);
            gui.setControlsEnabled(true);
        });
        waitForUserCommand();
        resumeVM();
    }

    // ========== Implémentation de DebuggerController ==========

    @Override
    public void onContinue() throws Exception {
        new ContinueCommand().execute(state);
        signalContinue();
    }

    @Override
    public void onStepOver() throws Exception {
        new StepOverCommand().execute(state);
        signalContinue();
    }

    @Override
    public void onStepInto() throws Exception {
        new StepCommand().execute(state);
        signalContinue();
    }

    @Override
    public void onStop() {
        stop();
        System.exit(0);
    }

    @Override
    public void onBreakpointToggle(String file, int line) throws Exception {
        new BreakCommand(file, line).execute(state);
        if (log != null) {
            log.info("Breakpoint set: %s:%d", file, line);
        }
    }
}