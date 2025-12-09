package commands;

import models.DebugFrame;

class FrameCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        DebugFrame frame = state.getContext().getCurrentFrame();
        if (frame == null) {
            return CommandResult.error("No current frame");
        }
        return CommandResult.success(frame);
    }
}
