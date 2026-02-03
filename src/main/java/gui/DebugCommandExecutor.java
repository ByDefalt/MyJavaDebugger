package gui;
import commands.*;
import models.DebuggerState;
public class DebugCommandExecutor {
    private final DebuggerState state;
    public DebugCommandExecutor(DebuggerState state) {
        this.state = state;
    }
    public CommandResult executeContinue() throws Exception {
        return new ContinueCommand().execute(state);
    }
    public CommandResult executeStepOver() throws Exception {
        return new StepOverCommand().execute(state);
    }
    public CommandResult executeStepInto() throws Exception {
        return new StepCommand().execute(state);
    }
    public CommandResult executeStepBack() throws Exception {
        return new BackCommand().execute(state);
    }
    public CommandResult executeBreakpointSet(String file, int line) throws Exception {
        return new BreakCommand(file, line).execute(state);
    }
}
