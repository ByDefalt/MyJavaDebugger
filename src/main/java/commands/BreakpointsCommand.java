package commands;

import models.Breakpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

class BreakpointsCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        Map<String, Breakpoint> bps = state.getBreakpoints();
        if (bps.isEmpty()) {
            return CommandResult.success("No breakpoints set", Collections.emptyList());
        }
        return CommandResult.success(new ArrayList<>(bps.values()));
    }
}
