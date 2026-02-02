package gui;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import commands.*;
import models.*;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Contrôleur du debugger avec GUI refactorisé (SRP)
 *
 * Responsabilité : Faire le lien entre la VM debuggée et l'interface graphique
 * Implémente ModernDebuggerGUI.DebuggerController pour respecter DIP
 */
public class ModernScriptableDebuggerGUI implements ModernDebuggerGUI.DebuggerController {

    private Class<?> debugClass;
    private VirtualMachine vm;
    private DebuggerState state;
    private ModernDebuggerGUI gui;

    private volatile boolean isRunning = true;
    private volatile boolean shouldContinue = false;
    private volatile boolean guiReady = false;
    private final Object lock = new Object();

    /**
     * Attache le debugger à une classe
     */
    public void attachTo(Class<?> debuggeeClass) {
        this.debugClass = debuggeeClass;

        // Créer et afficher la GUI sur l'EDT
        SwingUtilities.invokeLater(() -> {
            gui = new ModernDebuggerGUI();
            gui.setController(this);
            gui.setVisible(true);
            gui.appendOutput("[START] Debugging " + debugClass.getSimpleName() + "...\n");
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

        // Démarrer le debugger dans un thread séparé
        new Thread(this::startDebugging, "Debugger-Thread").start();
    }

    private void startDebugging() {
        try {
            vm = connectAndLaunchVM();
            state = new DebuggerState(vm);

            captureTargetOutput();
            enableClassPrepareRequest();
            startDebuggerLoop();

        } catch (Exception e) {
            e.printStackTrace();
            if (gui != null) {
                gui.appendOutput("Connection error: " + e.getMessage() + "\n");
            }
        }
    }

    private VirtualMachine connectAndLaunchVM() throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();
        args.get("main").setValue(debugClass.getName());
        args.get("options").setValue("-cp " + System.getProperty("java.class.path"));
        return connector.launch(args);
    }

    private void enableClassPrepareRequest() {
        ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
        r.addClassFilter(debugClass.getName());
        r.enable();
    }

    /**
     * Capture la sortie standard du programme debuggé
     */
    private void captureTargetOutput() {
        Process process = vm.process();
        if (process == null) return;

        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while (isRunning && (line = reader.readLine()) != null) {
                    final String output = line;
                    SwingUtilities.invokeLater(() -> gui.appendOutput(output + "\n"));
                }
            } catch (IOException e) {
                if (isRunning) {
                    gui.appendOutput("Output read error: " + e.getMessage() + "\n");
                }
            }
        }, "Output-Capture-Thread");

        outputThread.setDaemon(true);
        outputThread.start();
    }

    /**
     * Boucle principale du debugger
     */
    private void startDebuggerLoop() throws Exception {
        while (isRunning) {
            EventSet eventSet = vm.eventQueue().remove();
            boolean shouldResume = true;

            for (Event event : eventSet) {
                boolean eventNeedsWait = handleEvent(event);
                if (eventNeedsWait) {
                    shouldResume = false;
                }
            }

            if (shouldResume) {
                vm.resume();
            }
        }
    }

    /**
     * @return true si on doit attendre après cet événement (ne pas resume)
     */
    private boolean handleEvent(Event event) throws Exception {
        gui.appendOutput("[EVENT] " + event.getClass().getSimpleName() + "\n");

        if (event instanceof VMDisconnectEvent) {
            handleVMDisconnect();
            return false;
        } else if (event instanceof ClassPrepareEvent) {
            handleClassPrepare();
            return false; // On peut resume après ClassPrepare, le breakpoint est configuré
        } else if (event instanceof BreakpointEvent) {
            handleBreakpoint((BreakpointEvent) event);
            return true; // On attend une commande utilisateur
        } else if (event instanceof StepEvent) {
            handleStep((StepEvent) event);
            return true; // On attend une commande utilisateur
        }
        return false;
    }

    private void handleVMDisconnect() {
        SwingUtilities.invokeLater(() -> {
            gui.appendOutput("\n[END] Process finished.\n");
            gui.setControlsEnabled(false);
        });
        isRunning = false;
    }

    private void handleClassPrepare() {
        setInitialBreakpoint();
    }

    private void handleBreakpoint(BreakpointEvent event) throws Exception {
        Location loc = event.location();
        gui.appendOutput("[BREAK] Breakpoint hit at " + loc.sourceName() + ":" + loc.lineNumber() + "\n");
        updateUIAndWait(event.location(), event.thread());
        vm.resume(); // Resume après que l'utilisateur ait donné une commande
    }

    private void handleStep(StepEvent event) throws Exception {
        vm.eventRequestManager().deleteEventRequest(event.request());
        Location loc = event.location();
        gui.appendOutput("[STEP] at " + loc.sourceName() + ":" + loc.lineNumber() + "\n");
        updateUIAndWait(event.location(), event.thread());
        vm.resume(); // Resume après que l'utilisateur ait donné une commande
    }

    /**
     * Met à jour l'UI et attend une commande utilisateur
     */
    private void updateUIAndWait(Location loc, ThreadReference thread) throws Exception {
        state.updateContext(thread);

        SwingUtilities.invokeLater(() -> {
            gui.updateDebuggerState(state, loc, thread);
            gui.setControlsEnabled(true);
        });

        waitForUserCommand();
    }

    private void setInitialBreakpoint() {
        for (ReferenceType type : vm.allClasses()) {
            if (type.name().equals(debugClass.getName())) {
                try {
                    // Chercher la première ligne exécutable dans le main (généralement ligne 13+)
                    // Essayer plusieurs lignes pour trouver du code exécutable
                    for (int lineNum = 12; lineNum <= 20; lineNum++) {
                        List<Location> locs = type.locationsOfLine(lineNum);
                        if (!locs.isEmpty()) {
                            vm.eventRequestManager().createBreakpointRequest(locs.get(0)).enable();
                            gui.appendOutput("[OK] Initial breakpoint set at line " + lineNum + "\n");
                            return;
                        }
                    }
                    gui.appendOutput("[WARN] Could not find executable line for initial breakpoint\n");
                } catch (Exception e) {
                    gui.appendOutput("Could not set initial breakpoint: " + e.getMessage() + "\n");
                }
            }
        }
    }

    /**
     * Attend une commande de l'utilisateur
     */
    private void waitForUserCommand() {
        synchronized (lock) {
            shouldContinue = false;
            while (!shouldContinue && isRunning) {
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    /**
     * Signale que l'utilisateur a donné une commande
     */
    private void signalContinue() {
        synchronized (lock) {
            shouldContinue = true;
            lock.notifyAll();
        }
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
        isRunning = false;
        if (vm != null) {
            try {
                vm.exit(0);
            } catch (Exception ignored) {}
        }
        System.exit(0);
    }

    @Override
    public void onBreakpointToggle(String file, int line) throws Exception {
        new BreakCommand(file, line).execute(state);
        gui.appendOutput("Breakpoint set: " + file + ":" + line + "\n");
    }

    // ========== Point d'entrée ==========

    public static void main(String[] args) {
        // Exemple d'utilisation
        ModernScriptableDebuggerGUI debugger = new ModernScriptableDebuggerGUI();
        debugger.attachTo(dbg.JDISimpleDebuggee.class);
    }
}
