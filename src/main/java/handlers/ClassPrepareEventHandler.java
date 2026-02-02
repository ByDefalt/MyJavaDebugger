package handlers;

import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import models.DebuggerState;

/**
 * Handler pour l'événement de préparation de classe
 */
public class ClassPrepareEventHandler implements EventHandler<ClassPrepareEvent> {

    private final Class<?> debugClass;

    public ClassPrepareEventHandler(Class<?> debugClass) {
        this.debugClass = debugClass;
    }

    @Override
    public boolean canHandle(Event event) {
        return event instanceof ClassPrepareEvent;
    }

    @Override
    public EventHandlerResult handle(ClassPrepareEvent event, DebuggerState state) {
        System.out.println("Class loaded: " + debugClass.getName());

        if (state.isRecordingMode()) {
            setupMainMethodEntry(state);
        }

        return EventHandlerResult.continueExecution();
    }

    private void setupMainMethodEntry(DebuggerState state) {
        EventRequestManager erm = state.getVm().eventRequestManager();
        MethodEntryRequest methodEntryRequest = erm.createMethodEntryRequest();
        methodEntryRequest.addClassFilter(debugClass.getName());
        methodEntryRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        methodEntryRequest.enable();

        System.out.println("MethodEntryRequest configured for " + debugClass.getName());
    }
}
