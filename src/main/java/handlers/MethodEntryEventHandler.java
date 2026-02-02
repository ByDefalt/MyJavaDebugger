package handlers;

import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import managers.SnapshotRecorder;
import models.DebuggerState;

/**
 * Handler pour les événements d'entrée dans une méthode (SRP)
 */
public class MethodEntryEventHandler implements EventHandler<MethodEntryEvent> {

    private final Class<?> debugClass;

    public MethodEntryEventHandler(Class<?> debugClass) {
        this.debugClass = debugClass;
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof MethodEntryEvent;
    }

    @Override
    public EventHandlerResult handle(MethodEntryEvent event, DebuggerState state) throws Exception {
        if (state.isRecordingMode() && event.method().name().equals("main")) {
            return handleMainEntryRecording(event, state);
        } else {
            return handleNormalMethodEntry(event, state);
        }
    }

    private EventHandlerResult handleMainEntryRecording(MethodEntryEvent event, DebuggerState state) {
        // Créer un StepRequest automatique
        createAutoStepRequest(event.thread(), state);

        // Enregistrer le premier snapshot via SnapshotRecorder (SRP)
        SnapshotRecorder recorder = new SnapshotRecorder(state);
        recorder.recordSnapshot(event.thread());

        // Désactiver le MethodEntryRequest
        state.getVm().eventRequestManager().deleteEventRequest(event.request());

        return EventHandlerResult.continueExecution("Starting auto-recording from main() entry...");
    }

    private EventHandlerResult handleNormalMethodEntry(MethodEntryEvent event, DebuggerState state) throws Exception {
        Method method = event.method();

        for (String methodName : state.getMethodBreakpoints().keySet()) {
            if (method.name().equals(methodName)) {
                String message = "\n=== Method entry: " + method.name() + " ===\n" +
                        "Class: " + method.declaringType().name();

                state.updateContext(event.thread());
                return EventHandlerResult.waitForCommand(message);
            }
        }

        return EventHandlerResult.continueExecution();
    }

    private void createAutoStepRequest(ThreadReference thread, DebuggerState state) {
        EventRequestManager erm = state.getVm().eventRequestManager();
        StepRequest stepRequest = erm.createStepRequest(thread,
                StepRequest.STEP_LINE,
                StepRequest.STEP_INTO);

        stepRequest.addClassExclusionFilter("java.*");
        stepRequest.addClassExclusionFilter("javax.*");
        stepRequest.addClassExclusionFilter("sun.*");
        stepRequest.addClassExclusionFilter("com.sun.*");
        stepRequest.addClassExclusionFilter("jdk.*");

        stepRequest.enable();
    }
}
