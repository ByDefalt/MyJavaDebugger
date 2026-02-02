package execution;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.StepRequest;
import commands.CommandResult;
import models.DebuggerState;

/**
 * Stratégie d'exécution pour le mode live (exécution réelle)
 */
public class LiveExecutionStrategy implements ExecutionStrategy {

    @Override
    public boolean isApplicable(DebuggerState state) {
        return !state.isReplayMode();
    }

    @Override
    public CommandResult step(DebuggerState state) throws Exception {
        ThreadReference thread = state.getContext().getThread();
        StepRequest sr = state.getVm().eventRequestManager()
                .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);

        addStandardFilters(sr);
        sr.addCountFilter(1);
        sr.enable();

        return CommandResult.success("Step into next instruction", sr);
    }

    @Override
    public CommandResult stepOver(DebuggerState state) throws Exception {
        ThreadReference thread = state.getContext().getThread();
        StepRequest sr = state.getVm().eventRequestManager()
                .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER);

        addStandardFilters(sr);
        sr.addCountFilter(1);
        sr.enable();

        return CommandResult.success("Step over to next instruction", sr);
    }

    @Override
    public CommandResult continueExecution(DebuggerState state) throws Exception {
        return CommandResult.success("Continuing execution", null);
    }

    /**
     * Ajoute les filtres standard pour exclure les classes système
     */
    private void addStandardFilters(StepRequest sr) {
        sr.addClassExclusionFilter("java.*");
        sr.addClassExclusionFilter("javax.*");
        sr.addClassExclusionFilter("sun.*");
        sr.addClassExclusionFilter("com.sun.*");
        sr.addClassExclusionFilter("jdk.*");
    }
}
