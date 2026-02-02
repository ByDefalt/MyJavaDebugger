package commands;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.StepRequest;
import models.DebuggerState;
import models.ExecutionSnapshot;

public class StepOverCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        // En mode replay, step-over = avancer jusqu'à la prochaine ligne au même niveau de pile
        if (state.isReplayMode() && state.getExecutionHistory() != null) {
            ExecutionSnapshot current = state.getExecutionHistory().getCurrentSnapshot();
            if (current == null) {
                return CommandResult.error("No current execution state.");
            }

            int currentStackDepth = current.getStackFrames().size();

            // Avancer jusqu'à trouver un step avec une profondeur de pile <= actuelle
            while (state.getExecutionHistory().hasNext()) {
                state.getExecutionHistory().forward();
                ExecutionSnapshot next = state.getExecutionHistory().getCurrentSnapshot();

                if (next.getStackFrames().size() <= currentStackDepth) {
                    return new CommandResult(true, next.toDetailedString(), next);
                }
            }

            // Si on arrive à la fin sans trouver, rester au dernier
            ExecutionSnapshot last = state.getExecutionHistory().getCurrentSnapshot();
            return new CommandResult(true, "Reached end of execution\n" + last.toDetailedString(), last);
        }

        // Mode normal : exécution réelle
        ThreadReference thread = state.getContext().getThread();
        StepRequest sr = state.getVm().eventRequestManager()
                .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER);


        sr.addClassExclusionFilter("java.*");
        sr.addClassExclusionFilter("javax.*");
        sr.addClassExclusionFilter("sun.*");
        sr.addClassExclusionFilter("com.sun.*");
        sr.addClassExclusionFilter("jdk.*");

        sr.addCountFilter(1);
        sr.enable();
        return CommandResult.success("Step over current line", sr);
    }
}
