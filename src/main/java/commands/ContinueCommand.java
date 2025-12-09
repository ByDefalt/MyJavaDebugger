package commands;

import models.DebuggerState;

class ContinueCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        return CommandResult.success("Continuing execution", null);
    }
}
