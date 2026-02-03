package commands;
import models.CallStack;
import models.DebugFrame;
import models.DebuggerState;
class SenderCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        CallStack stack = state.getContext().getCallStack();
        if (stack == null || stack.getFrames().size() < 2) {
            return CommandResult.error("No sender frame available");
        }
        DebugFrame senderFrame = stack.getFrames().get(1);
        return CommandResult.success(senderFrame.getReceiver());
    }
}
