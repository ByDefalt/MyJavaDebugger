package gui;
import com.sun.jdi.*;
import com.sun.jdi.request.*;
import io.Logger;
import managers.SnapshotRecorder;
import models.DebuggerState;
public class RecordingEventHandler {
    private final DebuggerState state;
    private final Logger logger;
    private final VirtualMachine vm;
    private final Class<?> debugClass;
    public RecordingEventHandler(DebuggerState state, Logger logger, VirtualMachine vm, Class<?> debugClass) {
        this.state = state;
        this.logger = logger;
        this.vm = vm;
        this.debugClass = debugClass;
    }
    public boolean handleBreakpoint(Location loc, ThreadReference thread) throws Exception {
        recordSnapshot(thread);
        createStepRequest(thread);
        return false;
    }
    public boolean handleStep(Location loc, ThreadReference thread) throws Exception {
        recordSnapshot(thread);
        createStepRequest(thread);
        return false;
    }
    private void createStepRequest(ThreadReference thread) {
        try {
            EventRequestManager erm = vm.eventRequestManager();
            StepRequest stepRequest = erm.createStepRequest(
                thread,
                StepRequest.STEP_LINE,
                StepRequest.STEP_INTO
            );
            stepRequest.addClassFilter(debugClass.getName());
            stepRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            stepRequest.enable();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error creating step request", e);
            }
        }
    }
    private void recordSnapshot(ThreadReference thread) {
        SnapshotRecorder recorder = new SnapshotRecorder(state);
        recorder.recordSnapshot(thread);
        int count = recorder.getStepCount();
        if (count % 10 == 0 && logger != null) {
            logger.debug("📊 Recording... %d steps", count);
        }
    }
}
