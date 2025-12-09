package commands;

import models.CallStack;
import models.DebuggerState;

class StackCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        CallStack stack = state.getContext().getCallStack();
        if (stack == null) {
            return CommandResult.error("No call stack available");
        }
        return CommandResult.success(stack);
    }
}
