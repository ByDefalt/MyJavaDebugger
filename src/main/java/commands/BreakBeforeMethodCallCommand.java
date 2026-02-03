package commands;

import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.MethodEntryRequest;
import models.DebuggerState;

class BreakBeforeMethodCallCommand implements Command {
    private String methodName;

    public BreakBeforeMethodCallCommand(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        MethodEntryRequest req = state.getVm().eventRequestManager()
                .createMethodEntryRequest();

        for (ReferenceType type : state.getVm().allClasses()) {
            for (Method m : type.methods()) {
                if (m.name().equals(methodName)) {
                    req.addClassFilter(type);
                }
            }
        }

        req.enable();
        state.getMethodBreakpoints().put(methodName, req);

        return CommandResult.success("Method entry breakpoint set on " + methodName, req);
    }
}
