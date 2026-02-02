package commands;

import models.DebuggerState;
import models.ExecutionSnapshot;

/**
 * Commande pour reculer d'un pas dans l'historique d'exécution
 */
public class BackCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) {
        if (state.getExecutionHistory() == null || state.getExecutionHistory().isEmpty()) {
            return CommandResult.error("No execution history available. Run in recording mode first.");
        }

        if (!state.isReplayMode()) {
            return CommandResult.error("Back command only available in replay mode.");
        }

        if (!state.getExecutionHistory().hasPrevious()) {
            return CommandResult.error("Already at the beginning of execution history.");
        }

        state.getExecutionHistory().back();
        ExecutionSnapshot snapshot = state.getExecutionHistory().getCurrentSnapshot();

        return new CommandResult(true, snapshot.toDetailedString(), snapshot);
    }

}
