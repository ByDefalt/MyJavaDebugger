package commands;

import managers.BreakpointManager;
import models.Breakpoint;
import models.DebuggerState;

import java.util.Optional;

/**
 * Commande pour créer un breakpoint normal
 */
public class BreakCommand implements Command {

    private final String fileName;
    private final int lineNumber;

    public BreakCommand(String fileName, int lineNumber) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        BreakpointManager manager = new BreakpointManager(state);
        Optional<Breakpoint> bp = manager.createBreakpoint(fileName, lineNumber);

        if (bp.isPresent()) {
            String mode = state.isReplayMode() ? " (replay mode)" : "";
            return CommandResult.success("Breakpoint set" + mode, bp.get());
        }

        return CommandResult.error("Could not set breakpoint at " + fileName + ":" + lineNumber);
    }
}

