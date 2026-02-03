package dbg;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import models.DebuggerState;

import java.io.*;
import java.util.*;

/**
 * Classe abstraite pour tous les debuggers (Console et GUI)
 *
 * Template Method Pattern - définit le squelette du debugger
 * Les sous-classes implémentent uniquement les callbacks spécifiques
 *
 * Cette classe ne dépend PAS de Swing ni de la console (SRP + DIP)
 */
public abstract class AbstractDebugger {

    protected Class<?> debugClass;
    protected VirtualMachine vm;
    protected DebuggerState state;

    protected volatile boolean isRunning = true;
    protected volatile boolean shouldContinue = false;
    protected final Object lock = new Object();

    // ========== Template Method : Flux principal ==========

    /**
     * Attache le debugger à une classe (Template Method)
     */
    public void attachTo(Class<?> debuggeeClass) {
        this.debugClass = debuggeeClass;

        try {
            // 1. Initialiser l'interface (console ou GUI)
            initializeUI();

            // 2. Connecter à la VM
            vm = connectAndLaunchVM();
            state = new DebuggerState(vm);

            onInfo("[START] Debugging " + debugClass.getSimpleName() + "...");

            // 3. Configurer et démarrer
            captureTargetOutput();
            onBeforeStart();
            enableClassPrepareRequest();
            startDebuggerLoop();

        } catch (Exception e) {
            onError("Connection error: " + e.getMessage());
            // Le stacktrace est loggé via onError, pas besoin de printStackTrace
        }
    }

    // ========== Méthodes abstraites (callbacks) ==========

    /**
     * Initialise l'interface utilisateur (console ou GUI)
     */
    protected abstract void initializeUI();

    /**
     * Appelée avant de démarrer la boucle de debug
     */
    protected abstract void onBeforeStart();

    /**
     * Affiche un message d'information
     */
    protected abstract void onInfo(String message);

    /**
     * Affiche un message d'erreur
     */
    protected abstract void onError(String message);

    /**
     * Affiche la sortie du programme debuggé
     */
    protected abstract void onOutput(String output);

    /**
     * Appelée quand la VM se déconnecte
     */
    protected abstract void onVMDisconnect();

    /**
     * Appelée quand un breakpoint est atteint
     */
    protected abstract void onBreakpoint(Location loc, ThreadReference thread) throws Exception;

    /**
     * Appelée quand un step est terminé
     * @return true si on doit attendre une commande utilisateur
     */
    protected abstract boolean onStep(Location loc, ThreadReference thread) throws Exception;

    /**
     * Appelée quand on entre dans une méthode (pour le recording)
     * @return true si on doit attendre une commande utilisateur
     */
    protected boolean onMethodEntry(Location loc, ThreadReference thread) throws Exception {
        // Par défaut, ne fait rien - les sous-classes peuvent override
        return false;
    }

    /**
     * Appelée quand une classe est chargée
     */
    protected abstract void onClassPrepare(ReferenceType refType);

    // ========== Méthodes communes (implémentation partagée) ==========

    /**
     * Connecte et lance la VM debuggée
     */
    protected VirtualMachine connectAndLaunchVM() throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();
        args.get("main").setValue(debugClass.getName());
        args.get("options").setValue("-cp " + System.getProperty("java.class.path"));
        return connector.launch(args);
    }

    /**
     * Active la requête ClassPrepare
     */
    protected void enableClassPrepareRequest() {
        ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
        r.addClassFilter(debugClass.getName());
        r.enable();
    }

    /**
     * Capture la sortie standard du programme debuggé
     */
    protected void captureTargetOutput() {
        Process process = vm.process();
        if (process == null) return;

        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while (isRunning && (line = reader.readLine()) != null) {
                    onOutput(line + "\n");
                }
            } catch (IOException e) {
                if (isRunning) {
                    onError("Output read error: " + e.getMessage());
                }
            }
        }, "Output-Capture-Thread");

        outputThread.setDaemon(true);
        outputThread.start();
    }

    /**
     * Boucle principale du debugger
     */
    protected void startDebuggerLoop() throws Exception {
        while (isRunning) {
            EventSet eventSet = vm.eventQueue().remove();
            boolean shouldResume = true;

            for (Event event : eventSet) {
                boolean needsWait = handleEvent(event);
                if (needsWait) {
                    shouldResume = false;
                }
            }

            if (shouldResume && isRunning) {
                vm.resume();
            }
        }
    }

    /**
     * Gère un événement JDI
     * @return true si on doit attendre une commande utilisateur
     */
    protected boolean handleEvent(Event event) throws Exception {
        if (event instanceof VMDisconnectEvent) {
            onVMDisconnect();
            isRunning = false;
            return false;
        } else if (event instanceof ClassPrepareEvent) {
            ClassPrepareEvent cpe = (ClassPrepareEvent) event;
            onClassPrepare(cpe.referenceType());
            return false;
        } else if (event instanceof BreakpointEvent) {
            BreakpointEvent be = (BreakpointEvent) event;
            onBreakpoint(be.location(), be.thread());
            return true;
        } else if (event instanceof StepEvent) {
            StepEvent se = (StepEvent) event;
            vm.eventRequestManager().deleteEventRequest(se.request());
            return onStep(se.location(), se.thread());
        } else if (event instanceof MethodEntryEvent) {
            MethodEntryEvent me = (MethodEntryEvent) event;
            return onMethodEntry(me.location(), me.thread());
        }
        return false;
    }

    /**
     * Attend une commande de l'utilisateur
     */
    protected void waitForUserCommand() {
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
     * Signale que l'utilisateur a donné une commande et qu'on peut continuer
     */
    protected void signalContinue() {
        synchronized (lock) {
            shouldContinue = true;
            lock.notifyAll();
        }
    }

    /**
     * Resume la VM après une action utilisateur
     */
    protected void resumeVM() {
        if (vm != null) {
            vm.resume();
        }
    }

    /**
     * Arrête le debugger
     */
    public void stop() {
        isRunning = false;
        if (vm != null) {
            try {
                vm.exit(0);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Affiche la sortie du processus
     */
    protected void printProcessOutput() {
        try {
            Process process = vm.process();
            if (process != null) {
                InputStreamReader reader = new InputStreamReader(process.getInputStream());
                BufferedReader buffered = new BufferedReader(reader);
                String line;
                while ((line = buffered.readLine()) != null) {
                    onOutput(line + "\n");
                }
            }
        } catch (IOException e) {
            onError("Error reading VM output: " + e.getMessage());
        }
    }

    // ========== Getters ==========

    public DebuggerState getState() { return state; }
    public VirtualMachine getVm() { return vm; }
    public Class<?> getDebugClass() { return debugClass; }
    public boolean isRunning() { return isRunning; }
}
