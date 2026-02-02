package commands;

import models.DebuggerState;
import models.MethodInfo;

public class ArgumentsCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        MethodInfo method = state.getContext().getCurrentMethod();
        if (method == null) {
            return CommandResult.error("No current method");
        }
        return CommandResult.success(method.getArguments());
    }
}
