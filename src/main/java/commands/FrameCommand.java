package commands;

import models.DebugFrame;
import models.DebuggerState;
import models.ExecutionSnapshot;

class FrameCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        
        if (state.isReplayMode() && state.getExecutionHistory() != null) {
            ExecutionSnapshot snapshot = state.getExecutionHistory().getCurrentSnapshot();
            if (snapshot == null) {
                return CommandResult.error("No current execution state");
            }

            if (snapshot.getStackFrames().isEmpty()) {
                return CommandResult.error("No stack frames available");
            }

            ExecutionSnapshot.StackFrameSnapshot topFrame = snapshot.getStackFrames().get(0);
            StringBuilder sb = new StringBuilder();
            sb.append("Current Frame (from recorded state):\n");
            sb.append("  Location: ").append(topFrame.getSourceFile()).append(":").append(topFrame.getLineNumber()).append("\n");
            sb.append("  Method: ").append(topFrame.getClassName()).append(".").append(topFrame.getMethodName()).append("()\n");
            sb.append("  Local Variables:\n");

            if (snapshot.getLocalVariables().isEmpty()) {
                sb.append("    (none)\n");
            } else {
                for (var entry : snapshot.getLocalVariables().entrySet()) {
                    sb.append("    ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
                }
            }

            return CommandResult.success(sb.toString(), topFrame);
        }

        DebugFrame frame = state.getContext().getCurrentFrame();
        if (frame == null) {
            return CommandResult.error("No current frame");
        }
        return CommandResult.success(frame);
    }
}
