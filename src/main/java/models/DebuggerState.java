package models;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.MethodEntryRequest;
import java.util.HashMap;
import java.util.Map;
public class DebuggerState {
    private VirtualMachine vm;
    private ExecutionContext context;
    private Map<String, Breakpoint> breakpoints;
    private Map<String, MethodEntryRequest> methodBreakpoints;
    private boolean running;
    private ExecutionHistory executionHistory;
    private boolean replayMode;
    private boolean recordingMode;
    private final StringBuilder outputBuffer = new StringBuilder();
    private String lastCapturedOutput = "";
    public DebuggerState(VirtualMachine vm) {
        this.vm = vm;
        this.breakpoints = new HashMap<>();
        this.methodBreakpoints = new HashMap<>();
        this.running = true;
        this.executionHistory = new ExecutionHistory();
        this.replayMode = false;
        this.recordingMode = false;
    }
    public void updateContext(ThreadReference thread) throws IncompatibleThreadStateException {
        this.context = new ExecutionContext(thread);
    }
    public VirtualMachine getVm() { return vm; }
    public ExecutionContext getContext() { return context; }
    public Map<String, Breakpoint> getBreakpoints() { return breakpoints; }
    public Map<String, MethodEntryRequest> getMethodBreakpoints() { return methodBreakpoints; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public ExecutionHistory getExecutionHistory() { return executionHistory; }
    public boolean isReplayMode() { return replayMode; }
    public void setReplayMode(boolean replayMode) { this.replayMode = replayMode; }
    public boolean isRecordingMode() { return recordingMode; }
    public void setRecordingMode(boolean recordingMode) { this.recordingMode = recordingMode; }

    public void appendOutput(String text) {
        outputBuffer.append(text);
    }

    public String getAndResetOutput() {
        String output = outputBuffer.toString();
        lastCapturedOutput = output;
        outputBuffer.setLength(0);
        return output;
    }

    public String getLastCapturedOutput() {
        return lastCapturedOutput;
    }
}
