package execution;

import commands.CommandResult;
import models.Breakpoint;
import models.DebuggerState;
import models.ExecutionHistory;
import models.ExecutionSnapshot;

public class ReplayExecutionStrategy implements ExecutionStrategy {

    @Override
    public boolean isApplicable(DebuggerState state) {
        return state.isReplayMode() && state.getExecutionHistory() != null;
    }

    @Override
    public CommandResult step(DebuggerState state) throws Exception {
        ExecutionHistory history = state.getExecutionHistory();

        if (!history.hasNext()) {
            return CommandResult.error("Already at the end of execution history.");
        }

        history.forward();
        ExecutionSnapshot snapshot = history.getCurrentSnapshot();
        return new CommandResult(true, snapshot.toDetailedString(), snapshot);
    }

    @Override
    public CommandResult stepOver(DebuggerState state) throws Exception {
        ExecutionHistory history = state.getExecutionHistory();
        ExecutionSnapshot current = history.getCurrentSnapshot();

        if (current == null) {
            return CommandResult.error("No current execution state.");
        }

        int currentDepth = current.getStackFrames().size();

        while (history.hasNext()) {
            history.forward();
            ExecutionSnapshot next = history.getCurrentSnapshot();
            if (next.getStackFrames().size() <= currentDepth) {
                return new CommandResult(true, next.toDetailedString(), next);
            }
        }

        ExecutionSnapshot last = history.getCurrentSnapshot();
        return new CommandResult(true,
            "Reached end of execution\n" + last.toDetailedString(), last);
    }

    public CommandResult stepBack(DebuggerState state) throws Exception {
        ExecutionHistory history = state.getExecutionHistory();
        ExecutionSnapshot current = history.getCurrentSnapshot();

        if (current == null) {
            return CommandResult.error("No current execution state.");
        }

        if (!history.hasPrevious()) {
            return CommandResult.error("Already at the beginning of execution history.");
        }

        int currentDepth = current.getStackFrames().size();

        while (history.hasPrevious()) {
            history.back();
            ExecutionSnapshot prev = history.getCurrentSnapshot();
            if (prev.getStackFrames().size() <= currentDepth) {
                return new CommandResult(true, prev.toDetailedString(), prev);
            }
        }

        ExecutionSnapshot first = history.getCurrentSnapshot();
        return new CommandResult(true,
            "Reached beginning of execution\n" + first.toDetailedString(), first);
    }

    @Override
    public CommandResult continueExecution(DebuggerState state) throws Exception {
        ExecutionHistory history = state.getExecutionHistory();
        ExecutionSnapshot current = history.getCurrentSnapshot();

        if (current == null) {
            return CommandResult.error("No current execution state.");
        }

        while (history.hasNext()) {
            history.forward();
            ExecutionSnapshot next = history.getCurrentSnapshot();

            Breakpoint bp = findMatchingBreakpoint(state, next);
            if (bp != null) {
                bp.incrementHitCount();
                if (bp.shouldStop()) {
                    String key = next.getSourceFile() + ":" + next.getLineNumber();
                    return new CommandResult(true,
                        "Hit breakpoint at " + key + "\n" + next.toDetailedString(), next);
                }
            }
        }

        ExecutionSnapshot last = history.getCurrentSnapshot();
        return new CommandResult(true,
            "Reached end of execution (no breakpoint hit)\n" + last.toDetailedString(), last);
    }

    private Breakpoint findMatchingBreakpoint(DebuggerState state, ExecutionSnapshot snapshot) {
        String sourceFile = snapshot.getSourceFile();
        int lineNum = snapshot.getLineNumber();

        String keyWithExtension = sourceFile + ":" + lineNum;
        String keyWithoutExtension = sourceFile.replace(".java", "") + ":" + lineNum;

        if (state.getBreakpoints().containsKey(keyWithExtension)) {
            return state.getBreakpoints().get(keyWithExtension);
        }
        if (state.getBreakpoints().containsKey(keyWithoutExtension)) {
            return state.getBreakpoints().get(keyWithoutExtension);
        }

        return null;
    }
}
