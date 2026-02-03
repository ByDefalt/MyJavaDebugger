package commands;
import com.sun.jdi.ObjectReference;
import models.DebugFrame;
import models.DebuggerState;
class ReceiverCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        DebugFrame frame = state.getContext().getCurrentFrame();
        if (frame == null) {
            return CommandResult.error("No current frame");
        }
        ObjectReference receiver = frame.getReceiver();
        if (receiver == null) {
            return CommandResult.error("No receiver (static method?)");
        }
        return CommandResult.success(receiver);
    }
}
