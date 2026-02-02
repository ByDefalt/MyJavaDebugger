package commands;

import models.CallStack;
import models.DebuggerState;
import models.ExecutionSnapshot;

class StackCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        // En mode replay, utiliser le snapshot de l'historique
        if (state.isReplayMode() && state.getExecutionHistory() != null) {
            ExecutionSnapshot snapshot = state.getExecutionHistory().getCurrentSnapshot();
            if (snapshot == null) {
                return CommandResult.error("No current execution state");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Call Stack (from recorded state):\n");
            for (ExecutionSnapshot.StackFrameSnapshot frame : snapshot.getStackFrames()) {
                sb.append("  ").append(frame).append("\n");
            }
            return CommandResult.success(sb.toString(), snapshot.getStackFrames());
        }

        // Mode normal : utiliser le contexte actuel
        CallStack stack = state.getContext().getCallStack();
        if (stack == null) {
            return CommandResult.error("No call stack available");
        }
        return CommandResult.success(stack);
    }
}
