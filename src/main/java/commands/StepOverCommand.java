package commands;

import execution.ExecutionStrategy;
import execution.LiveExecutionStrategy;
import execution.ReplayExecutionStrategy;
import models.DebuggerState;

import java.util.Arrays;
import java.util.List;

/**
 * Commande Step Over - utilise le pattern Strategy pour déléguer selon le mode
 */
public class StepOverCommand implements Command {

    private static final List<ExecutionStrategy> STRATEGIES = Arrays.asList(
            new ReplayExecutionStrategy(),
            new LiveExecutionStrategy()
    );

    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        for (ExecutionStrategy strategy : STRATEGIES) {
            if (strategy.isApplicable(state)) {
                return strategy.stepOver(state);
            }
        }
        return CommandResult.error("No applicable execution strategy found");
    }
}
