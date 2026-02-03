package managers;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import models.Breakpoint;
import models.DebuggerState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public class BreakpointManager {
    private final DebuggerState state;
    public BreakpointManager(DebuggerState state) {
        this.state = state;
    }
    public Optional<Breakpoint> createBreakpoint(String fileName, int lineNumber) {
        return createBreakpoint(fileName, lineNumber, Breakpoint.BreakpointType.NORMAL, 0);
    }
    public Optional<Breakpoint> createBreakpointOnce(String fileName, int lineNumber) {
        return createBreakpoint(fileName, lineNumber, Breakpoint.BreakpointType.ONCE, 0);
    }
    public Optional<Breakpoint> createBreakpointOnCount(String fileName, int lineNumber, int count) {
        return createBreakpoint(fileName, lineNumber, Breakpoint.BreakpointType.ON_COUNT, count);
    }
    public Optional<Breakpoint> createBreakpoint(String fileName, int lineNumber,
            Breakpoint.BreakpointType type, int count) {
        String normalizedFileName = normalizeFileName(fileName);
        String key = normalizedFileName + ":" + lineNumber;
        if (state.isReplayMode()) {
            Breakpoint bp = new Breakpoint(normalizedFileName, lineNumber, null, type, count);
            state.getBreakpoints().put(key, bp);
            return Optional.of(bp);
        }
        VirtualMachine vm = state.getVm();
        for (ReferenceType refType : vm.allClasses()) {
            try {
                String sourceName = refType.sourceName();
                if (sourceName.equals(normalizedFileName) || sourceName.equals(fileName)) {
                    List<Location> locs = refType.locationsOfLine(lineNumber);
                    if (!locs.isEmpty()) {
                        Location loc = locs.get(0);
                        BreakpointRequest req = vm.eventRequestManager().createBreakpointRequest(loc);
                        req.enable();
                        Breakpoint bp = new Breakpoint(normalizedFileName, lineNumber, req, type, count);
                        state.getBreakpoints().put(key, bp);
                        return Optional.of(bp);
                    }
                }
            } catch (Exception e) {
            }
        }
        return Optional.empty();
    }
    public boolean removeBreakpoint(String fileName, int lineNumber) {
        String key = normalizeFileName(fileName) + ":" + lineNumber;
        Breakpoint bp = state.getBreakpoints().remove(key);
        if (bp != null && bp.getRequest() != null) {
            bp.getRequest().disable();
            state.getVm().eventRequestManager().deleteEventRequest(bp.getRequest());
            return true;
        }
        return bp != null;
    }
    public Optional<Breakpoint> getBreakpoint(String sourceFile, int lineNumber) {
        String keyWithExtension = normalizeFileName(sourceFile) + ":" + lineNumber;
        String keyWithoutExtension = sourceFile.replace(".java", "") + ":" + lineNumber;
        Breakpoint bp = state.getBreakpoints().get(keyWithExtension);
        if (bp == null) {
            bp = state.getBreakpoints().get(keyWithoutExtension);
        }
        return Optional.ofNullable(bp);
    }
    public Map<String, Breakpoint> getAllBreakpoints() {
        return state.getBreakpoints();
    }
    public void clearAllBreakpoints() {
        for (Breakpoint bp : state.getBreakpoints().values()) {
            if (bp.getRequest() != null) {
                bp.getRequest().disable();
                state.getVm().eventRequestManager().deleteEventRequest(bp.getRequest());
            }
        }
        state.getBreakpoints().clear();
    }
    private String normalizeFileName(String fileName) {
        if (!fileName.endsWith(".java")) {
            return fileName + ".java";
        }
        return fileName;
    }
}
