package commands;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.StepRequest;
import models.DebuggerState;
import models.ExecutionSnapshot;

public class StepCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        // En mode replay, naviguer dans l'historique au lieu d'exécuter
        if (state.isReplayMode() && state.getExecutionHistory() != null) {
            if (!state.getExecutionHistory().hasNext()) {
                return CommandResult.error("Already at the end of execution history.");
            }

            state.getExecutionHistory().forward();
            ExecutionSnapshot snapshot = state.getExecutionHistory().getCurrentSnapshot();
            return new CommandResult(true, snapshot.toDetailedString(), snapshot);
        }

        // Mode normal : exécution réelle
        ThreadReference thread = state.getContext().getThread();
        StepRequest sr = state.getVm().eventRequestManager()
                .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);

        sr.addClassExclusionFilter("java.*");
        sr.addClassExclusionFilter("javax.*");
        sr.addClassExclusionFilter("sun.*");
        sr.addClassExclusionFilter("com.sun.*");
        sr.addClassExclusionFilter("jdk.*");

        sr.addCountFilter(1);
        sr.enable();
        return CommandResult.success("Step into next instruction", sr);
    }
}
