package commands;


import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.BreakpointRequest;
import models.Breakpoint;

import java.util.List;

class BreakOnCountCommand implements Command {
    private String fileName;
    private int lineNumber;
    private int count;

    public BreakOnCountCommand(String fileName, int lineNumber, int count) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.count = count;
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
                        Breakpoint.BreakpointType.ON_COUNT, count);
                state.getBreakpoints().put(fileName + ":" + lineNumber, bp);

                return CommandResult.success("Count breakpoint set", bp);
            }
        }
        return CommandResult.error("Class not found for file: " + fileName);
    }
}

