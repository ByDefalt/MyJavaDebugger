package commands;

import managers.BreakpointManager;
import models.Breakpoint;
import models.DebuggerState;

import java.util.Optional;

/**
 * Commande pour créer un breakpoint one-shot (s'arrête une seule fois)
 */
class BreakOnceCommand implements Command {

    private final String fileName;
    private final int lineNumber;

    public BreakOnceCommand(String fileName, int lineNumber) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        BreakpointManager manager = new BreakpointManager(state);
        Optional<Breakpoint> bp = manager.createBreakpointOnce(fileName, lineNumber);

        if (bp.isPresent()) {
            return CommandResult.success("One-time breakpoint set", bp.get());
        }

        return CommandResult.error("Could not set breakpoint at " + fileName + ":" + lineNumber);
    }
}