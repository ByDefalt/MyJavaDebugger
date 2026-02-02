package handlers;

import com.sun.jdi.event.Event;
import com.sun.jdi.event.VMDisconnectEvent;
import models.DebuggerState;

/**
 * Handler pour l'événement de déconnexion de la VM
 */
public class VMDisconnectEventHandler implements EventHandler<VMDisconnectEvent> {

    @Override
    public boolean canHandle(Event event) {
        return event instanceof VMDisconnectEvent;
    }

    @Override
    public EventHandlerResult handle(VMDisconnectEvent event, DebuggerState state) {
        if (state.isRecordingMode()) {
            state.getExecutionHistory().completeRecording();
            state.setRecordingMode(false);
            state.setReplayMode(true);

            StringBuilder message = new StringBuilder();
            message.append("\n=== Program terminated ===\n");
            message.append("\n=== RECORDING COMPLETE ===\n");
            message.append("Total steps recorded: ").append(state.getExecutionHistory().size()).append("\n");
            message.append("\nYou can now navigate through execution history with:\n");
            message.append("  - forward: go to next step\n");
            message.append("  - back: go to previous step\n");
            message.append("  - history: show execution history overview\n");

            if (state.getExecutionHistory().size() > 0) {
                message.append("\n").append(state.getExecutionHistory().getCurrentSnapshot().toDetailedString());
            }

            return EventHandlerResult.enterReplayMode(message.toString());
        }

        return EventHandlerResult.stop("\n=== Program terminated ===");
    }
}
