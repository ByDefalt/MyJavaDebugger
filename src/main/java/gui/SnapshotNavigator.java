package gui;
import models.DebugFrame;
import models.ExecutionSnapshot;
import java.util.ArrayList;
import java.util.List;
/**
 * Responsible for navigating through execution snapshots and converting snapshot data.
 * Follows Single Responsibility Principle.
 */
public class SnapshotNavigator {
    private List<ExecutionSnapshot> snapshots = new ArrayList<>();
    public void setSnapshots(List<ExecutionSnapshot> snapshots) {
        this.snapshots = snapshots != null ? snapshots : new ArrayList<>();
    }
    public List<ExecutionSnapshot> getSnapshots() {
        return snapshots;
    }
    public boolean hasSnapshots() {
        return !snapshots.isEmpty();
    }
    public ExecutionSnapshot findSnapshotByStepNumber(int stepNumber) {
        for (ExecutionSnapshot snapshot : snapshots) {
            if (snapshot.getStepNumber() == stepNumber) {
                return snapshot;
            }
        }
        return null;
    }
    public int findSnapshotIndexByStepNumber(int stepNumber) {
        for (int i = 0; i < snapshots.size(); i++) {
            if (snapshots.get(i).getStepNumber() == stepNumber) {
                return i;
            }
        }
        return -1;
    }
    public List<DebugFrame> convertStackFrames(List<ExecutionSnapshot.StackFrameSnapshot> stackFrames) {
        List<DebugFrame> frames = new ArrayList<>();
        for (ExecutionSnapshot.StackFrameSnapshot sf : stackFrames) {
            DebugFrame frame = new DebugFrame(
                    sf.getClassName() + "." + sf.getMethodName() + "()",
                    sf.getSourceFile(),
                    sf.getLineNumber()
            );
            frames.add(frame);
        }
        return frames;
    }
    public String buildShortClassName(String fullClassName) {
        return fullClassName.contains(".") ?
                fullClassName.substring(fullClassName.lastIndexOf('.') + 1) : fullClassName;
    }
}
