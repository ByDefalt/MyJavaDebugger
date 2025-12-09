package commands;


import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.BreakpointRequest;
import models.Breakpoint;

import java.util.List;

class BreakCommand implements Command {
    private String fileName;
    private int lineNumber;

    public BreakCommand(String fileName, int lineNumber) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        for (ReferenceType t : state.getVm().allClasses()) {
            if (t.sourceName().equals(fileName)) {
                List<Location> locs = t.locationsOfLine(lineNumber);
                if (locs.isEmpty()) {
                    return CommandResult.error("No code at line " + lineNumber);
                }

                Location loc = locs.get(0);
                BreakpointRequest req = state.getVm().eventRequestManager()
                        .createBreakpointRequest(loc);
                req.enable();

                Breakpoint bp = new Breakpoint(fileName, lineNumber, req,
                        Breakpoint.BreakpointType.NORMAL);
                state.getBreakpoints().put(fileName + ":" + lineNumber, bp);

                return CommandResult.success("Breakpoint set", bp);
            }
        }
        return CommandResult.error("Class not found for file: " + fileName);
    }
}

