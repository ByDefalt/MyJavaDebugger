package commands;
import managers.BreakpointManager;
import models.Breakpoint;
import models.DebuggerState;
import java.util.Optional;
class BreakOnCountCommand implements Command {
    private final String fileName;
    private final int lineNumber;
    private final int count;
    public BreakOnCountCommand(String fileName, int lineNumber, int count) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.count = count;
    }
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        BreakpointManager manager = new BreakpointManager(state);
        Optional<Breakpoint> bp = manager.createBreakpointOnCount(fileName, lineNumber, count);
        if (bp.isPresent()) {
            return CommandResult.success("Count breakpoint set (activates after " + count + " hits)", bp.get());
        }
        return CommandResult.error("Could not set breakpoint at " + fileName + ":" + lineNumber);
    }
}
