package gui;
import com.sun.jdi.*;
import commands.*;
import dbg.AbstractDebugger;
import io.GUILogger;
import io.Logger;
import models.ExecutionSnapshot;
import javax.swing.*;
import java.util.*;
public class ScriptableDebuggerGUI extends AbstractDebugger
        implements DebuggerGUI.DebuggerController {
    private DebuggerGUI gui;
    private Logger log;
    private boolean recordingPhase = true;
    private final int initialBreakpointLine;
    private final ConsoleRebuilder consoleRebuilder;
    private BreakpointInitializer breakpointInitializer;
    private RecordingEventHandler recordingEventHandler;
    private ReplayEventHandler replayEventHandler;
    private DebugCommandExecutor commandExecutor;

    public ScriptableDebuggerGUI() {
        this(-1);
    }

    public ScriptableDebuggerGUI(int initialBreakpointLine) {
        this.initialBreakpointLine = initialBreakpointLine;
        this.consoleRebuilder = new ConsoleRebuilder();
    }

    @Override
    protected void initializeUI() {
        try {
            UIInitializer initializer = new UIInitializer();
            UIInitializer.UIContext context = initializer.initializeAndWait(10);

            this.gui = context.gui;
            this.log = context.logger;

            SwingUtilities.invokeLater(() -> {
                gui.setController(this);
                gui.setVisible(true);
            });

            breakpointInitializer = new BreakpointInitializer(log);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("UI initialization interrupted", e);
        }
    }
    @Override
    protected void onBeforeStart() {
        state.setRecordingMode(true);
        recordingPhase = true;

        recordingEventHandler = new RecordingEventHandler(state, log, vm, debugClass);
        replayEventHandler = new ReplayEventHandler(state, log, gui, v -> waitForUserCommand(), this::resumeVM);
        commandExecutor = new DebugCommandExecutor(state);

        if (log != null) {
            log.info("📝 Recording execution... Please wait.");
        }
        SwingUtilities.invokeLater(() -> gui.setControlsEnabled(false));
    }
    @Override
    protected void onInfo(String message) {
        if (log != null) {
            log.info(message);
        }
    }
    @Override
    protected void onError(String message) {
        if (log != null) {
            log.error(message);
        }
    }
    @Override
    protected void onOutput(String output) {
        if (recordingPhase && state != null) {
            state.appendOutput(output);
        }
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.appendOutput(output));
        }
    }
    @Override
    protected void onVMDisconnect() {
        if (log != null) {
            int count = state.getExecutionHistory().size();
            log.info("Recording complete! %d steps recorded.", count);
            log.info("Entering replay mode - use controls to navigate through execution.");
        }
        recordingPhase = false;
        state.setRecordingMode(false);
        state.setReplayMode(true);
        state.getExecutionHistory().goToStart();
        List<ExecutionSnapshot> snapshots = state.getExecutionHistory().getAllSnapshots();
        SwingUtilities.invokeLater(() -> {
            gui.clearOutput();
            gui.setExecutionSnapshots(snapshots);
            gui.setControlsEnabled(true);
            if (!snapshots.isEmpty()) {
                gui.updateFromSnapshot(snapshots.get(0));
            }
        });
    }
    @Override
    protected boolean onBreakpoint(Location loc, ThreadReference thread) throws Exception {
        if (recordingPhase) {
            return recordingEventHandler.handleBreakpoint(loc, thread);
        } else {
            return replayEventHandler.handleBreakpoint(loc, thread);
        }
    }

    @Override
    protected boolean onStep(Location loc, ThreadReference thread) throws Exception {
        if (recordingPhase) {
            return recordingEventHandler.handleStep(loc, thread);
        } else {
            return replayEventHandler.handleStep(loc, thread);
        }
    }

    @Override
    protected void onClassPrepare(ReferenceType refType) {
        if (log != null) {
            log.debug("Class loaded: %s", refType.name());
        }

        if (breakpointInitializer != null) {
            breakpointInitializer.setInitialBreakpoint(vm, refType, debugClass, initialBreakpointLine);
        }
    }

    @Override
    public void onContinue() throws Exception {
        CommandResult result = commandExecutor.executeContinue();
        handleCommandResult(result);
    }
    @Override
    public void onStepOver() throws Exception {
        CommandResult result = commandExecutor.executeStepOver();
        handleCommandResult(result);
    }
    @Override
    public void onStepInto() throws Exception {
        CommandResult result = commandExecutor.executeStepInto();
        handleCommandResult(result);
    }
    @Override
    public void onStepBack() throws Exception {
        CommandResult result = commandExecutor.executeStepBack();
        handleCommandResult(result);
    }
    private void handleCommandResult(CommandResult result) {
        if (state.isReplayMode()) {
            if (result.hasSnapshot()) {
                ExecutionSnapshot snapshot = result.getSnapshot();
                consoleRebuilder.rebuildUpToStep(gui, state.getExecutionHistory().getAllSnapshots(),
                                                 snapshot.getStepNumber());
                gui.updateFromSnapshot(snapshot);
                if (log != null) {
                    log.debug("Step #%d: %s:%d",
                        snapshot.getStepNumber(), snapshot.getSourceFile(), snapshot.getLineNumber());
                }
            }
            if (result.getMessage() != null && !result.getMessage().isEmpty()) {
                gui.appendDebugLog(result.getMessage() + "\n");
            }
        } else {
            signalContinue();
        }
    }

    @Override
    public void onStop() {
        stop();
        System.exit(0);
    }
    @Override
    public void onBreakpointToggle(String file, int line) throws Exception {
        String key = file + ":" + line;
        if (state.getBreakpoints().containsKey(key)) {
            state.getBreakpoints().remove(key);
            if (log != null) {
                log.info("Breakpoint removed: %s", key);
            }
            gui.getSourceCodePanel().removeBreakpoint(line);
        } else {
            commandExecutor.executeBreakpointSet(file, line);
            if (log != null) {
                log.info("Breakpoint set: %s", key);
            }
        }
    }
    @Override
    public void onNavigateToStep(int snapshotIndex) {
        state.getExecutionHistory().goToStep(snapshotIndex);
        if (log != null) {
            log.debug("⏱ Time travel: now at step #%d", snapshotIndex);
        }
    }
}