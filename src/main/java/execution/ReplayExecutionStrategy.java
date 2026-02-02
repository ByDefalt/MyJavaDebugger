package execution;

import commands.CommandResult;
import models.Breakpoint;
import models.DebuggerState;
import models.ExecutionHistory;
import models.ExecutionSnapshot;

/**
 * Stratégie d'exécution pour le mode replay (navigation dans l'historique)
 */
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

        // Chercher le prochain état avec une profondeur <= actuelle
        while (history.hasNext()) {
            history.forward();
            ExecutionSnapshot next = history.getCurrentSnapshot();
            if (next.getStackFrames().size() <= currentDepth) {
                return new CommandResult(true, next.toDetailedString(), next);
            }
        }

        // Si on atteint la fin, rester sur le dernier état
        ExecutionSnapshot last = history.getCurrentSnapshot();
        return new CommandResult(true,
            "Reached end of execution\n" + last.toDetailedString(), last);
    }

    @Override
    public CommandResult continueExecution(DebuggerState state) throws Exception {
        ExecutionHistory history = state.getExecutionHistory();
        ExecutionSnapshot current = history.getCurrentSnapshot();

        if (current == null) {
            return CommandResult.error("No current execution state.");
        }

        // Chercher le prochain breakpoint dans l'historique
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

        // Si pas de breakpoint trouvé, aller à la fin
        ExecutionSnapshot last = history.getCurrentSnapshot();
        return new CommandResult(true,
            "Reached end of execution (no breakpoint hit)\n" + last.toDetailedString(), last);
    }

    /**
     * Cherche un breakpoint correspondant au snapshot
     */
    private Breakpoint findMatchingBreakpoint(DebuggerState state, ExecutionSnapshot snapshot) {
        String sourceFile = snapshot.getSourceFile();
        int lineNum = snapshot.getLineNumber();

        // Créer les clés possibles
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
