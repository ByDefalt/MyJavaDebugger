package gui;
import models.ExecutionSnapshot;
import java.util.List;
/**
 * Responsible for rebuilding the program output console based on execution history.
 * Follows Single Responsibility Principle.
 */
public class ConsoleRebuilder {
    public void rebuildUpToStep(DebuggerGUI gui, List<ExecutionSnapshot> snapshots, int targetStep) {
        gui.clearOutput();
        for (ExecutionSnapshot snap : snapshots) {
            if (snap.getStepNumber() <= targetStep) {
                String output = snap.getOutputText();
                if (output != null && !output.isEmpty()) {
                    gui.appendOutput(output);
                }
            }
        }
    }
    public void rebuildFull(DebuggerGUI gui, List<ExecutionSnapshot> snapshots) {
        gui.clearOutput();
        for (ExecutionSnapshot snap : snapshots) {
            String output = snap.getOutputText();
            if (output != null && !output.isEmpty()) {
                gui.appendOutput(output);
            }
        }
    }
}
