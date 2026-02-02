package commands;

import models.DebuggerState;

/**
 * Commande pour afficher l'historique d'exécution
 */
public class HistoryCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) {
        if (state.getExecutionHistory() == null || state.getExecutionHistory().isEmpty()) {
            return CommandResult.error("No execution history available.");
        }

        return new CommandResult(true, state.getExecutionHistory().toString(),
                                state.getExecutionHistory());
    }
}
