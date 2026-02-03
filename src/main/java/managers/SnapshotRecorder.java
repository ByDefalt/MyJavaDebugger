package managers;
import com.sun.jdi.ThreadReference;
import models.DebuggerState;
import models.ExecutionSnapshot;
public class SnapshotRecorder {
    private final DebuggerState state;
    public SnapshotRecorder(DebuggerState state) {
        this.state = state;
    }
    public void recordSnapshot(ThreadReference thread) {
        try {
            int stepNumber = state.getExecutionHistory().size();
            ExecutionSnapshot snapshot = new ExecutionSnapshot(stepNumber, thread);
            state.getExecutionHistory().addSnapshot(snapshot);
        } catch (Exception e) {
        }
    }
    public int getStepCount() {
        return state.getExecutionHistory().size();
    }
    public boolean shouldLogProgress() {
        int count = getStepCount();
        return count > 0 && count % 100 == 0;
    }
}
