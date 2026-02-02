package commands;

import models.DebuggerState;
import models.ExecutionSnapshot;
import models.Breakpoint;

public class ContinueCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        // En mode replay, continuer jusqu'au prochain breakpoint ou la fin
        if (state.isReplayMode() && state.getExecutionHistory() != null) {
            ExecutionSnapshot current = state.getExecutionHistory().getCurrentSnapshot();
            if (current == null) {
                return CommandResult.error("No current execution state.");
            }

            // Chercher le prochain breakpoint dans l'historique
            while (state.getExecutionHistory().hasNext()) {
                state.getExecutionHistory().forward();
                ExecutionSnapshot next = state.getExecutionHistory().getCurrentSnapshot();

                // Vérifier si on est sur un breakpoint (essayer avec et sans .java)
                String sourceFile = next.getSourceFile();
                int lineNum = next.getLineNumber();

                // Créer les clés possibles
                String keyWithExtension = sourceFile + ":" + lineNum;
                String keyWithoutExtension = sourceFile.replace(".java", "") + ":" + lineNum;

                Breakpoint bp = null;
                String matchedKey = null;

                // Essayer avec l'extension
                if (state.getBreakpoints().containsKey(keyWithExtension)) {
                    bp = state.getBreakpoints().get(keyWithExtension);
                    matchedKey = keyWithExtension;
                }
                // Essayer sans l'extension
                else if (state.getBreakpoints().containsKey(keyWithoutExtension)) {
                    bp = state.getBreakpoints().get(keyWithoutExtension);
                    matchedKey = keyWithoutExtension;
                }

                if (bp != null) {
                    bp.incrementHitCount();
                    if (bp.shouldStop()) {
                        return new CommandResult(true,
                            "Hit breakpoint at " + matchedKey + "\n" + next.toDetailedString(), next);
                    }
                }
            }

            // Si pas de breakpoint trouvé, aller à la fin
            ExecutionSnapshot last = state.getExecutionHistory().getCurrentSnapshot();
            return new CommandResult(true,
                "Reached end of execution (no breakpoint hit)\n" + last.toDetailedString(), last);
        }

        // Mode normal : exécution réelle
        return CommandResult.success("Continuing execution", null);
    }
}
