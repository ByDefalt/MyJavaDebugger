package handlers;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;
import managers.SnapshotRecorder;
import models.DebuggerState;

/**
 * Handler pour les événements de step (SRP : gère uniquement le dispatch des step events)
 */
public class StepEventHandler implements EventHandler<StepEvent> {

    @Override
    public boolean canHandle(Event event) {
        return event instanceof StepEvent;
    }

    @Override
    public EventHandlerResult handle(StepEvent event, DebuggerState state) throws Exception {
        if (state.isRecordingMode()) {
            return handleRecordingMode(event, state);
        } else {
            return handleNormalMode(event, state);
        }
    }

    private EventHandlerResult handleRecordingMode(StepEvent event, DebuggerState state) {
        // Déléguer l'enregistrement au SnapshotRecorder (SRP)
        SnapshotRecorder recorder = new SnapshotRecorder(state);
        recorder.recordSnapshot(event.thread());

        // Retourner un message de progression si nécessaire
        if (recorder.shouldLogProgress()) {
            return EventHandlerResult.continueExecution(
                "... Recorded " + recorder.getStepCount() + " steps ...");
        }
        return EventHandlerResult.continueExecution();
    }

    private EventHandlerResult handleNormalMode(StepEvent event, DebuggerState state) throws Exception {
        String message = "\nStepped to: " + event.location().sourceName()
                + ":" + event.location().lineNumber();

        // Supprimer la StepRequest après usage
        state.getVm().eventRequestManager().deleteEventRequest(event.request());

        state.updateContext(event.thread());
        return EventHandlerResult.waitForCommand(message);
    }
}
