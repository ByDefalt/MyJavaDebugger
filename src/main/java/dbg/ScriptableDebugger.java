package dbg;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import commands.*;
import models.*;
import java.io.*;
import java.util.*;

public class ScriptableDebugger {
    private Class debugClass;
    private VirtualMachine vm;
    private DebuggerState state;
    private CommandInterpreter interpreter;
    private int stepCounter;
    private boolean autoRecord;

    public ScriptableDebugger() {
        this.interpreter = new CommandInterpreter();
        this.stepCounter = 0;
        this.autoRecord = false;
    }

    public ScriptableDebugger(boolean autoRecord) {
        this.interpreter = new CommandInterpreter();
        this.stepCounter = 0;
        this.autoRecord = autoRecord;
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

    public void attachTo(Class debuggeeClass) {
        this.debugClass = debuggeeClass;
        try {
            vm = connectAndLaunchVM();
            state = new DebuggerState(vm);

            if (autoRecord) {
                state.setRecordingMode(true);
                System.out.println("=== AUTO-RECORDING MODE ENABLED ===");
                System.out.println("The debugger will automatically step through ALL code and record execution states.");
            }

            enableClassPrepareRequest(vm);
            startDebugger();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
        r.addClassFilter(debugClass.getName());
        r.enable();
    }

    private void handleUserCommand(ThreadReference thread) {
        System.out.print("\ndbg> ");
        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();

        if (input.trim().isEmpty()) {
            return;
        }

        try {
            // Mettre à jour le contexte
            state.updateContext(thread);

            // Parser et exécuter la commande
            Command command = interpreter.parse(input);
            CommandResult result = command.execute(state);

            // Afficher le résultat
            displayResult(result);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void displayResult(CommandResult result) {
        if (!result.isSuccess()) {
            System.err.println("ERROR: " + result.getMessage());
            return;
        }

        if (!result.getMessage().isEmpty()) {
            System.out.println(result.getMessage());
        }

        Object data = result.getData();
        if (data == null) {
            return;
        }

        // Afficher selon le type de données
        if (data instanceof Variable) {
            System.out.println(data);
        } else if (data instanceof List) {
            List<?> list = (List<?>) data;
            if (list.isEmpty()) {
                System.out.println("(empty)");
            } else {
                for (Object item : list) {
                    System.out.println("  " + item);
                }
            }
        } else if (data instanceof DebugFrame) {
            System.out.println("Current frame: " + data);
        } else if (data instanceof CallStack) {
            System.out.println(data);
        } else if (data instanceof MethodInfo) {
            System.out.println("Method: " + data);
        } else if (data instanceof ObjectReference) {
            ObjectReference obj = (ObjectReference) data;
            System.out.println(obj.referenceType().name() + "@" + obj.uniqueID());
        } else if (data instanceof Breakpoint) {
            System.out.println("Breakpoint: " + data);
        } else if (data instanceof ExecutionHistory) {
            // Ne rien afficher - déjà affiché dans le message
            return;
        } else if (data instanceof ExecutionSnapshot) {
            // Ne rien afficher - déjà affiché dans le message
            return;
        } else {
            System.out.println(data);
        }
    }

    private void handleBreakpoint(BreakpointEvent event) throws IncompatibleThreadStateException, AbsentInformationException {
        Location loc = event.location();
        System.out.println("\n=== Breakpoint hit ===");
        System.out.println("Location: " + loc.sourceName() + ":" + loc.lineNumber());
        System.out.println("Method: " + loc.method().name());

        // Vérifier si c'est un breakpoint spécial
        String key = loc.sourceName() + ":" + loc.lineNumber();
        Breakpoint bp = state.getBreakpoints().get(key);

        if (bp != null) {
            bp.incrementHitCount();

            if (!bp.shouldStop()) {
                System.out.println("Breakpoint condition not met, continuing...");
                return;
            }

            // Si c'est un breakpoint "once", le désactiver
            if (bp.getType() == Breakpoint.BreakpointType.ONCE) {
                bp.getRequest().disable();
                state.getBreakpoints().remove(key);
                System.out.println("One-time breakpoint removed");
            }
        }

        // Attendre commande utilisateur
        handleUserCommand(event.thread());
    }

    private void handleMethodEntry(MethodEntryEvent event) throws IncompatibleThreadStateException {
        Method method = event.method();

        // Vérifier si on attend ce method entry
        for (String methodName : state.getMethodBreakpoints().keySet()) {
            if (method.name().equals(methodName)) {
                System.out.println("\n=== Method entry: " + method.name() + " ===");
                System.out.println("Class: " + method.declaringType().name());
                handleUserCommand(event.thread());
                return;
            }
        }
    }
    /**
     * Configure un MethodEntryRequest pour démarrer l'enregistrement dès l'entrée dans main()
     */
    private void setMainMethodEntry() {
        EventRequestManager erm = vm.eventRequestManager();
        MethodEntryRequest methodEntryRequest = erm.createMethodEntryRequest();

        // Filtrer uniquement pour la classe debuggée
        methodEntryRequest.addClassFilter(debugClass.getName());

        // Suspendre le thread quand on entre dans une méthode
        methodEntryRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        methodEntryRequest.enable();

        System.out.println("MethodEntryRequest configured for " + debugClass.getName());
    }

    /**
     * Crée une StepRequest pour automatiquement step-in à chaque instruction
     */
    private void createAutoStepRequest(ThreadReference thread) {
        EventRequestManager erm = vm.eventRequestManager();
        StepRequest stepRequest = erm.createStepRequest(thread,
                StepRequest.STEP_LINE,  // Step ligne par ligne
                StepRequest.STEP_INTO); // Entrer dans les méthodes

        // Filtrer pour ne step que dans notre classe debuggée (et ses dépendances)
        // On peut ajouter des filtres pour exclure les classes système
        stepRequest.addClassExclusionFilter("java.*");
        stepRequest.addClassExclusionFilter("javax.*");
        stepRequest.addClassExclusionFilter("sun.*");
        stepRequest.addClassExclusionFilter("com.sun.*");
        stepRequest.addClassExclusionFilter("jdk.*");

        stepRequest.enable();
    }

    /**
     * Enregistre un snapshot de l'état d'exécution actuel
     */
    private void recordSnapshot(ThreadReference thread) {
        try {
            ExecutionSnapshot snapshot = new ExecutionSnapshot(stepCounter++, thread);
            state.getExecutionHistory().addSnapshot(snapshot);

            if (stepCounter % 100 == 0) {
                System.out.println("... Recorded " + stepCounter + " steps ...");
            }
        } catch (Exception e) {
            System.err.println("Error recording snapshot: " + e.getMessage());
        }
    }





    public void startDebugger() throws InterruptedException, AbsentInformationException {
        System.out.println("=== Scriptable Debugger Started ===");
        System.out.println("Available commands: " + interpreter.getAvailableCommands());

        while (true) {
            EventSet eventSet = vm.eventQueue().remove();

            for (Event event : eventSet) {
                try {
                    if (event instanceof VMDisconnectEvent) {
                        System.out.println("\n=== Program terminated ===");

                        // Si en mode recording, finaliser l'enregistrement
                        if (state.isRecordingMode()) {
                            state.getExecutionHistory().completeRecording();
                            System.out.println("\n=== RECORDING COMPLETE ===");
                            System.out.println("Total steps recorded: " + state.getExecutionHistory().size());
                            System.out.println("\nYou can now navigate through execution history with:");
                            System.out.println("  - forward: go to next step");
                            System.out.println("  - back: go to previous step");
                            System.out.println("  - history: show execution history overview");

                            // Passer en mode replay
                            state.setRecordingMode(false);
                            state.setReplayMode(true);

                            // Afficher le premier état
                            if (state.getExecutionHistory().size() > 0) {
                                System.out.println("\n" + state.getExecutionHistory().getCurrentSnapshot().toDetailedString());
                            }

                            // Entrer en mode interactif pour naviguer dans l'historique
                            replayMode();
                        }

                        printProcessOutput();
                        return;
                    }

                    if (event instanceof ClassPrepareEvent) {
                        System.out.println("Class loaded: " + debugClass.getName());
                        // En mode recording, configurer un MethodEntryRequest pour démarrer dès main()
                        if (state.isRecordingMode()) {
                            setMainMethodEntry();
                        }
                    }

                    if (event instanceof BreakpointEvent) {
                        // Mode normal (pas de recording)
                        handleBreakpoint((BreakpointEvent) event);
                    }

                    if (event instanceof StepEvent) {
                        StepEvent se = (StepEvent) event;

                        if (state.isRecordingMode()) {
                            // En mode recording : enregistrer automatiquement et continuer
                            recordSnapshot(se.thread());
                            // NE PAS supprimer la StepRequest pour continuer à enregistrer
                        } else {
                            // En mode normal : afficher et attendre commande
                            System.out.println("\nStepped to: " + se.location().sourceName()
                                    + ":" + se.location().lineNumber());

                            // Supprimer la StepRequest après usage
                            vm.eventRequestManager().deleteEventRequest(event.request());

                            handleUserCommand(se.thread());
                        }
                    }

                    if (event instanceof MethodEntryEvent) {
                        MethodEntryEvent mee = (MethodEntryEvent) event;

                        if (state.isRecordingMode() && mee.method().name().equals("main")) {
                            System.out.println("Starting auto-recording from main() entry...");
                            // Créer un StepRequest automatique
                            createAutoStepRequest(mee.thread());
                            // Enregistrer le premier snapshot
                            recordSnapshot(mee.thread());
                            // Désactiver le MethodEntryRequest maintenant qu'on a démarré
                            vm.eventRequestManager().deleteEventRequest(event.request());
                        } else {
                            // Mode normal
                            handleMethodEntry(mee);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error handling event: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            vm.resume();
        }
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
        System.out.println("\n=== REPLAY MODE ===");
        System.out.println("You can now use standard debugger commands to navigate the recorded execution:");
        System.out.println("  step        - step into (go to next recorded state)");
        System.out.println("  step-over   - step over (skip to next state at same or lower stack depth)");
        System.out.println("  continue    - continue until breakpoint or end");
        System.out.println("  break <file> <line> - set a breakpoint");
        System.out.println("  breakpoints - list breakpoints");
        System.out.println("  back        - go back one step");
        System.out.println("  forward     - go forward one step");
        System.out.println("  history     - show execution overview");
        System.out.println("  stack       - show call stack");
        System.out.println("  frame       - show current frame");
        System.out.println("  quit        - exit replay mode");

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("\ndbg> ");
            String input = sc.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equals("quit") || input.equals("exit")) {
                System.out.println("Exiting replay mode.");
                break;
            }

            try {
                Command command = interpreter.parse(input);
                CommandResult result = command.execute(state);
                displayResult(result);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}