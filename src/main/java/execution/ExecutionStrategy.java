package execution;

import commands.CommandResult;
import models.DebuggerState;

public interface ExecutionStrategy {

    CommandResult step(DebuggerState state) throws Exception;

    CommandResult stepOver(DebuggerState state) throws Exception;

    CommandResult stepBack(DebuggerState state) throws Exception;

    CommandResult continueExecution(DebuggerState state) throws Exception;

    boolean isApplicable(DebuggerState state);
}
