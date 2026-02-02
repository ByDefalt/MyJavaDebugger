package handlers;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import models.Breakpoint;
import models.DebuggerState;

/**
 * Handler pour les événements de breakpoint
 */
public class BreakpointEventHandler implements EventHandler<BreakpointEvent> {

    @Override
    public boolean canHandle(Event event) {
        return event instanceof BreakpointEvent;
    }

    @Override
    public EventHandlerResult handle(BreakpointEvent event, DebuggerState state)
            throws IncompatibleThreadStateException, AbsentInformationException {

        Location loc = event.location();
        StringBuilder message = new StringBuilder();
        message.append("\n=== Breakpoint hit ===\n");
        message.append("Location: ").append(loc.sourceName()).append(":").append(loc.lineNumber()).append("\n");
        message.append("Method: ").append(loc.method().name());

        String key = loc.sourceName() + ":" + loc.lineNumber();
        Breakpoint bp = state.getBreakpoints().get(key);

        if (bp != null) {
            bp.incrementHitCount();

            if (!bp.shouldStop()) {
                return EventHandlerResult.continueExecution("Breakpoint condition not met, continuing...");
            }

            // Si c'est un breakpoint "once", le désactiver
            if (bp.getType() == Breakpoint.BreakpointType.ONCE) {
                bp.getRequest().disable();
                state.getBreakpoints().remove(key);
                message.append("\nOne-time breakpoint removed");
            }
        }

        state.updateContext(event.thread());
        return EventHandlerResult.waitForCommand(message.toString());
    }
}
