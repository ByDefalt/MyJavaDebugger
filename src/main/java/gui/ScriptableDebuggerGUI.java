package gui;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import commands.*;
import dbg.AbstractDebugger;
import io.GUILogger;
import io.Logger;
import managers.SnapshotRecorder;
import models.ExecutionSnapshot;

import javax.swing.*;
import java.util.*;

public class ScriptableDebuggerGUI extends AbstractDebugger
        implements DebuggerGUI.DebuggerController {

    private DebuggerGUI gui;
    private Logger log;
    private volatile boolean guiReady = false;
    private boolean recordingPhase = true;

    @Override
    protected void initializeUI() {
        SwingUtilities.invokeLater(() -> {
            gui = new DebuggerGUI();
            gui.setController(this);
            gui.setVisible(true);

            log = new GUILogger(gui::appendOutput, Logger.Level.DEBUG);

            guiReady = true;
        });

        while (!guiReady) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    protected void onBeforeStart() {
        state.setRecordingMode(true);
        recordingPhase = true;
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
        
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.appendOutput(output));
        }
    }

    @Override
    protected void onVMDisconnect() {
        if (log != null) {
            int count = state.getExecutionHistory().size();
            log.info("✅ Recording complete! %d steps recorded.", count);
            log.info("🎮 Entering replay mode - use controls to navigate through execution.");
        }

        recordingPhase = false;
        state.setRecordingMode(false);
        state.setReplayMode(true);
        state.getExecutionHistory().goToStart();

        List<ExecutionSnapshot> snapshots = state.getExecutionHistory().getAllSnapshots();
        SwingUtilities.invokeLater(() -> {
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
            recordSnapshot(thread);
            createStepRequest(thread);
            return false;
        } else {
            if (log != null) {
                log.info("Breakpoint at %s:%d", loc.sourceName(), loc.lineNumber());
            }
            updateUIAndWait(loc, thread);
            return true;
        }
    }

    @Override
    protected boolean onStep(Location loc, ThreadReference thread) throws Exception {
        if (recordingPhase) {
            recordSnapshot(thread);
            createStepRequest(thread);
            return false;
        } else {
            if (log != null) {
                log.debug("Step at %s:%d", loc.sourceName(), loc.lineNumber());
            }
            updateUIAndWait(loc, thread);
            return true;
        }
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
        }
    }

    private void recordSnapshot(ThreadReference thread) {
        SnapshotRecorder recorder = new SnapshotRecorder(state);
        recorder.recordSnapshot(thread);

        int count = recorder.getStepCount();
        if (count % 10 == 0 && log != null) {
            log.debug("📊 Recording... %d steps", count);
        }
    }

    @Override
    protected void onClassPrepare(ReferenceType refType) {
        if (log != null) {
            log.debug("Class loaded: %s", refType.name());
        }
        setInitialBreakpoint();
    }

    private void setInitialBreakpoint() {
        if (log != null) {
            log.debug("Looking for class: %s", debugClass.getName());
            log.debug("All classes count: %d", vm.allClasses().size());
        }

        for (ReferenceType type : vm.allClasses()) {
            if (type.name().equals(debugClass.getName())) {
                if (log != null) {
                    log.debug("Found class: %s", type.name());
                }
                try {
                    // EN DUR TODO() A MODIFIER
                    for (int lineNum = 13; lineNum <= 15; lineNum++) {
                        List<Location> locs = type.locationsOfLine(lineNum);
                        if (!locs.isEmpty()) {
                            vm.eventRequestManager().createBreakpointRequest(locs.get(0)).enable();
                            if (log != null) {
                                log.info("Breakpoint set at line %d", lineNum);
                            }
                            return;
                        }
                    }
                    if (log != null) {
                        log.warn("No executable lines found in range 13-25");
                    }
                } catch (Exception e) {
                    if (log != null) {
                        log.error("Error setting breakpoint", e);
                    }
                }
            }
        }
        if (log != null) {
            log.warn("Class not found in VM classes");
        }
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
        waitForUserCommand();
        resumeVM();
    }

    @Override
    public void onContinue() throws Exception {
        CommandResult result = new ContinueCommand().execute(state);
        handleCommandResult(result);
    }

    @Override
    public void onStepOver() throws Exception {
        CommandResult result = new StepOverCommand().execute(state);
        handleCommandResult(result);
    }

    @Override
    public void onStepInto() throws Exception {
        CommandResult result = new StepCommand().execute(state);
        handleCommandResult(result);
    }

    @Override
    public void onStepBack() throws Exception {
        CommandResult result = new BackCommand().execute(state);
        handleCommandResult(result);
    }

    private void handleCommandResult(CommandResult result) {
        if (state.isReplayMode()) {
            if (result.hasSnapshot()) {
                ExecutionSnapshot snapshot = result.getSnapshot();
                gui.updateFromSnapshot(snapshot);
                if (log != null) {
                    log.debug("Step #%d: %s:%d",
                        snapshot.getStepNumber(), snapshot.getSourceFile(), snapshot.getLineNumber());
                }
            }
            if (result.getMessage() != null && !result.getMessage().isEmpty()) {
                gui.appendOutput(result.getMessage() + "\n");
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
            new BreakCommand(file, line).execute(state);
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