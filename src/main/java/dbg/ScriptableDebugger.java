package dbg;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import commands.*;
import handlers.*;
import io.*;
import models.*;
import java.io.*;
import java.util.*;

/**
 * Debugger scriptable refactoré selon les principes SOLID :
 * - SRP : Responsabilités séparées (handlers, IO, commands)
 * - OCP : Extensible via EventHandlerRegistry
 * - DIP : Dépend d'abstractions (InputReader, ResultPresenter)
 */
public class ScriptableDebugger {
    private Class<?> debugClass;
    private VirtualMachine vm;
    private DebuggerState state;
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

    public VirtualMachine connectAndLaunchVM() throws IOException,
            IllegalConnectorArgumentsException, VMStartException {
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager()
                .defaultConnector();
        Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
        arguments.get("main").setValue(debugClass.getName());
        arguments.get("options").setValue("-cp " + System.getProperty("java.class.path"));
        return launchingConnector.launch(arguments);
    }

    public void attachTo(Class<?> debuggeeClass) {
        this.debugClass = debuggeeClass;
        try {
            vm = connectAndLaunchVM();
            state = new DebuggerState(vm);

            // Enregistrer les handlers d'événements (OCP)
            registerEventHandlers();

            if (autoRecord) {
                state.setRecordingMode(true);
                presenter.info("=== AUTO-RECORDING MODE ENABLED ===");
                presenter.info("The debugger will automatically step through ALL code and record execution states.");
            }

            enableClassPrepareRequest(vm);
            startDebugger();
        } catch (Exception e) {
            presenter.error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Enregistre les handlers d'événements (Open/Closed Principle)
     * Pour ajouter un nouveau type d'événement, créer un nouveau handler et l'enregistrer ici
     */
    private void registerEventHandlers() {
        eventHandlerRegistry.register(new VMDisconnectEventHandler());
        eventHandlerRegistry.register(new ClassPrepareEventHandler(debugClass));
        eventHandlerRegistry.register(new BreakpointEventHandler());
        eventHandlerRegistry.register(new StepEventHandler());
        eventHandlerRegistry.register(new MethodEntryEventHandler(debugClass));
    }

    public void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
        r.addClassFilter(debugClass.getName());
        r.enable();
    }


    public void startDebugger() throws InterruptedException, AbsentInformationException {
        presenter.info("=== Scriptable Debugger Started ===");
        presenter.info("Available commands: " + interpreter.getAvailableCommands());

        while (true) {
            EventSet eventSet = vm.eventQueue().remove();

            for (Event event : eventSet) {
                try {
                    // Dispatcher l'événement aux handlers (OCP)
                    EventHandlerResult result = eventHandlerRegistry.dispatch(event, state);

                    // Afficher le message si présent
                    if (!result.getMessage().isEmpty()) {
                        presenter.info(result.getMessage());
                    }

                    // Traiter l'action demandée
                    switch (result.getAction()) {
                        case STOP:
                            printProcessOutput();
                            return;

                        case ENTER_REPLAY_MODE:
                            replayMode();
                            printProcessOutput();
                            return;

                        case WAIT_FOR_COMMAND:
                            handleUserCommandLoop();
                            break;

                        case CONTINUE:
                        default:
                            // Continuer normalement
                            break;
                    }

                } catch (Exception e) {
                    presenter.error("Error handling event: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            vm.resume();
        }
    }

    /**
     * Boucle de commandes utilisateur (attend une commande de continuation)
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

                // Si la commande est de type navigation (step, continue), sortir de la boucle
                if (isNavigationCommand(input)) {
                    break;
                }
            } catch (Exception e) {
                presenter.error(e.getMessage());
            }
        }
    }

    /**
     * Vérifie si la commande est une commande de navigation
     */
    private boolean isNavigationCommand(String input) {
        String cmd = input.trim().split("\\s+")[0];
        return cmd.equals("step") || cmd.equals("step-over") || cmd.equals("continue");
    }

    public void printProcessOutput() {
        try {
            InputStreamReader reader = new InputStreamReader(vm.process().getInputStream());
            OutputStreamWriter writer = new OutputStreamWriter(System.out);
            reader.transferTo(writer);
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error reading VM output: " + e.getMessage());
        }
    }

    /**
     * Mode replay : permet à l'utilisateur de naviguer dans l'historique d'exécution
     * avec les commandes de débogage habituelles (step, step-over, continue, break, etc.)
     */
    private void replayMode() {
        presenter.info("\n=== REPLAY MODE ===");
        presenter.info("You can now use standard debugger commands to navigate the recorded execution:");
        presenter.info("  step        - step into (go to next recorded state)");
        presenter.info("  step-over   - step over (skip to next state at same or lower stack depth)");
        presenter.info("  continue    - continue until breakpoint or end");
        presenter.info("  break <file> <line> - set a breakpoint");
        presenter.info("  breakpoints - list breakpoints");
        presenter.info("  back        - go back one step");
        presenter.info("  forward     - go forward one step");
        presenter.info("  history     - show execution overview");
        presenter.info("  stack       - show call stack");
        presenter.info("  frame       - show current frame");
        presenter.info("  quit        - exit replay mode");

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