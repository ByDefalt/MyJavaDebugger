package dbg;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import commands.*;
import handlers.*;
import io.*;
import models.*;

import java.util.*;

/**
 * Debugger scriptable en mode console
 *
 * Hérite de AbstractDebugger pour le code commun
 * Utilise InputReader et ResultPresenter pour l'I/O (DIP)
 */
public class ScriptableDebugger extends AbstractDebugger {

    private CommandInterpreter interpreter;
    private boolean autoRecord;

    // Injection de dépendances (DIP)
    private final InputReader inputReader;
    private final ResultPresenter presenter;
    private final EventHandlerRegistry eventHandlerRegistry;

    public ScriptableDebugger() {
        this(false, new ConsoleInputReader(), new ConsoleResultPresenter());
    }

    public ScriptableDebugger(boolean autoRecord) {
        this(autoRecord, new ConsoleInputReader(), new ConsoleResultPresenter());
    }

    /**
     * Constructeur avec injection de dépendances complète
     */
    public ScriptableDebugger(boolean autoRecord, InputReader inputReader, ResultPresenter presenter) {
        this.interpreter = new CommandInterpreter();
        this.autoRecord = autoRecord;
        this.inputReader = inputReader;
        this.presenter = presenter;
        this.eventHandlerRegistry = new EventHandlerRegistry();
    }

    // ========== Implémentation des callbacks abstraits ==========

    @Override
    protected void initializeUI() {
        // Console : rien à initialiser
    }

    @Override
    protected void onBeforeStart() {
        // Enregistrer les handlers d'événements (OCP)
        eventHandlerRegistry.register(new VMDisconnectEventHandler());
        eventHandlerRegistry.register(new ClassPrepareEventHandler(debugClass));
        eventHandlerRegistry.register(new BreakpointEventHandler());
        eventHandlerRegistry.register(new StepEventHandler());
        eventHandlerRegistry.register(new MethodEntryEventHandler(debugClass));

        if (autoRecord) {
            state.setRecordingMode(true);
            presenter.info("=== AUTO-RECORDING MODE ENABLED ===");
            presenter.info("The debugger will automatically step through ALL code and record execution states.");
        }

        presenter.info("=== Scriptable Debugger Started ===");
        presenter.info("Available commands: " + interpreter.getAvailableCommands());
    }

    @Override
    protected void onInfo(String message) {
        presenter.info(message);
    }

    @Override
    protected void onError(String message) {
        presenter.error(message);
    }

    @Override
    protected void onOutput(String output) {
        System.out.print(output);
    }

    @Override
    protected void onVMDisconnect() {
        presenter.info("\n=== Program terminated ===");

        if (state.isRecordingMode()) {
            state.getExecutionHistory().completeRecording();
            presenter.info("\n=== RECORDING COMPLETE ===");
            presenter.info("Total steps recorded: " + state.getExecutionHistory().size());
            presenter.info("\nYou can now navigate through execution history with:");
            presenter.info("  - forward: go to next step");
            presenter.info("  - back: go to previous step");
            presenter.info("  - history: show execution history overview");

            state.setRecordingMode(false);
            state.setReplayMode(true);

            if (state.getExecutionHistory().size() > 0) {
                presenter.info("\n" + state.getExecutionHistory().getCurrentSnapshot().toDetailedString());
            }

            replayMode();
        }

        printProcessOutput();
    }

    @Override
    protected void onBreakpoint(Location loc, ThreadReference thread) throws Exception {
        presenter.info("\n=== Breakpoint hit ===");
        presenter.info("Location: " + loc.sourceName() + ":" + loc.lineNumber());
        presenter.info("Method: " + loc.method().name());

        state.updateContext(thread);
        handleUserCommandLoop();
        resumeVM();
    }

    @Override
    protected void onStep(Location loc, ThreadReference thread) throws Exception {
        if (state.isRecordingMode()) {
            // Mode recording : enregistrer et continuer
            managers.SnapshotRecorder recorder = new managers.SnapshotRecorder(state);
            recorder.recordSnapshot(thread);
            if (recorder.shouldLogProgress()) {
                presenter.info("... Recorded " + recorder.getStepCount() + " steps ...");
            }
            resumeVM();
        } else {
            // Mode normal : afficher et attendre
            presenter.info("\nStepped to: " + loc.sourceName() + ":" + loc.lineNumber());
            state.updateContext(thread);
            handleUserCommandLoop();
            resumeVM();
        }
    }

    @Override
    protected void onClassPrepare(ReferenceType refType) {
        presenter.info("Class loaded: " + debugClass.getName());

        if (state.isRecordingMode()) {
            // Configurer le step automatique pour le recording
            setupAutoRecording();
        }
    }

    // ========== Méthodes spécifiques au mode console ==========

    private void setupAutoRecording() {
        EventRequestManager erm = vm.eventRequestManager();
        MethodEntryRequest methodEntryRequest = erm.createMethodEntryRequest();
        methodEntryRequest.addClassFilter(debugClass.getName());
        methodEntryRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        methodEntryRequest.enable();
        presenter.info("MethodEntryRequest configured for " + debugClass.getName());
    }

    /**
     * Boucle de commandes utilisateur
     */
    private void handleUserCommandLoop() {
        while (true) {
            String input = inputReader.readLine("\ndbg> ");

            if (input == null || input.trim().isEmpty()) {
                continue;
            }

            try {
                Command command = interpreter.parse(input);
                CommandResult result = command.execute(state);
                presenter.displayResult(result);

                if (isNavigationCommand(input)) {
                    break;
                }
            } catch (Exception e) {
                presenter.error(e.getMessage());
            }
        }
    }

    private boolean isNavigationCommand(String input) {
        String cmd = input.trim().split("\\s+")[0];
        return cmd.equals("step") || cmd.equals("step-over") || cmd.equals("continue");
    }

    /**
     * Mode replay pour naviguer dans l'historique
     */
    private void replayMode() {
        presenter.info("\n=== REPLAY MODE ===");
        presenter.info("Commands: step, step-over, continue, back, forward, history, stack, frame, quit");

        while (true) {
            String input = inputReader.readLine("\ndbg> ");

            if (input == null || input.isEmpty()) {
                continue;
            }

            if (input.equals("quit") || input.equals("exit")) {
                presenter.info("Exiting replay mode.");
                break;
            }

            try {
                Command command = interpreter.parse(input);
                CommandResult result = command.execute(state);
                presenter.displayResult(result);
            } catch (Exception e) {
                presenter.error(e.getMessage());
            }
        }
    }
}

