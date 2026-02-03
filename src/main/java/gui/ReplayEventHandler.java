package gui;
import com.sun.jdi.*;
import io.Logger;
import models.DebuggerState;
import models.ExecutionSnapshot;
import javax.swing.SwingUtilities;
import java.util.function.Consumer;
public class ReplayEventHandler {
    private final DebuggerState state;
    private final Logger logger;
    private final DebuggerGUI gui;
    private final Consumer<Void> waitForUserCommand;
    private final Runnable resumeVM;
    public ReplayEventHandler(DebuggerState state, Logger logger, DebuggerGUI gui, 
                              Consumer<Void> waitForUserCommand, Runnable resumeVM) {
        this.state = state;
        this.logger = logger;
        this.gui = gui;
        this.waitForUserCommand = waitForUserCommand;
        this.resumeVM = resumeVM;
    }
    public boolean handleBreakpoint(Location loc, ThreadReference thread) throws Exception {
        if (logger != null) {
            logger.info("Breakpoint at %s:%d", loc.sourceName(), loc.lineNumber());
        }
        updateUIAndWait(loc, thread);
        return true;
    }
    public boolean handleStep(Location loc, ThreadReference thread) throws Exception {
        if (logger != null) {
            logger.debug("Step at %s:%d", loc.sourceName(), loc.lineNumber());
        }
        updateUIAndWait(loc, thread);
        return true;
    }
    private void updateUIAndWait(Location loc, ThreadReference thread) throws Exception {
        state.updateContext(thread);
        ExecutionSnapshot currentSnapshot = state.getExecutionHistory().getCurrentSnapshot();
        SwingUtilities.invokeLater(() -> {
            gui.updateDebuggerState(state, loc, thread);
            gui.setControlsEnabled(true);
            if (currentSnapshot != null) {
                gui.getVariablesPanel().updateFromSnapshots(currentSnapshot.getVariablesForFrame(0));
            }
        });
        waitForUserCommand.accept(null);
        resumeVM.run();
    }
}
