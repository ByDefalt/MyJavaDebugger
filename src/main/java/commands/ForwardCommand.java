package commands;

import models.DebuggerState;
import models.ExecutionSnapshot;

public class ForwardCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) {
        if (state.getExecutionHistory() == null || state.getExecutionHistory().isEmpty()) {
            return CommandResult.error("No execution history available. Run in recording mode first.");
        }

        if (!state.isReplayMode()) {
            return CommandResult.error("Forward command only available in replay mode.");
        }

        if (!state.getExecutionHistory().hasNext()) {
            return CommandResult.error("Already at the end of execution history.");
        }

        state.getExecutionHistory().forward();
        ExecutionSnapshot snapshot = state.getExecutionHistory().getCurrentSnapshot();

        return new CommandResult(true, snapshot.toDetailedString(), snapshot);
    }

}
