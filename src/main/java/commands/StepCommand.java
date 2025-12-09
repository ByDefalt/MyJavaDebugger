package commands;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.StepRequest;

class StepCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        ThreadReference thread = state.getContext().getThread();
        StepRequest sr = state.getVm().eventRequestManager()
                .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
        sr.addCountFilter(1);
        sr.enable();
        return CommandResult.success("Step into next instruction", sr);
    }
}
