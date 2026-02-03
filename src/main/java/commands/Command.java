package commands;
import models.DebuggerState;
public interface Command {
    CommandResult execute(DebuggerState state) throws Exception;
}
