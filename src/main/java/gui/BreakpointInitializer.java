package gui;
import com.sun.jdi.*;
import com.sun.jdi.request.BreakpointRequest;
import io.Logger;
import java.util.List;
/**
 * Responsible for setting initial breakpoints in the debugged program.
 * Follows Single Responsibility Principle.
 */
public class BreakpointInitializer {
    private final Logger logger;
    public BreakpointInitializer(Logger logger) {
        this.logger = logger;
    }
    public boolean setInitialBreakpoint(VirtualMachine vm, ReferenceType type, 
                                        Class<?> debugClass, int initialBreakpointLine) {
        if (!type.name().equals(debugClass.getName())) {
            return false;
        }
        if (logger != null) {
            logger.debug("Found class: %s", type.name());
        }
        try {
            if (initialBreakpointLine == -1) {
                return setBreakpointAtMainMethod(vm, type);
            } else {
                return setBreakpointAtLine(vm, type, initialBreakpointLine);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error setting breakpoint", e);
            }
            return false;
        }
    }
    private boolean setBreakpointAtMainMethod(VirtualMachine vm, ReferenceType type) {
        Location mainLocation = findMainMethodFirstLine(type);
        if (mainLocation != null) {
            BreakpointRequest request = vm.eventRequestManager().createBreakpointRequest(mainLocation);
            request.enable();
            if (logger != null) {
                logger.info("Breakpoint set at first line of main: %d", mainLocation.lineNumber());
            }
            return true;
        } else {
            if (logger != null) {
                logger.warn("Could not find main method or its first executable line");
            }
            return false;
        }
    }
    private boolean setBreakpointAtLine(VirtualMachine vm, ReferenceType type, int lineNumber) {
        try {
            List<Location> locs = type.locationsOfLine(lineNumber);
            if (!locs.isEmpty()) {
                BreakpointRequest request = vm.eventRequestManager().createBreakpointRequest(locs.get(0));
                request.enable();
                if (logger != null) {
                    logger.info("Breakpoint set at line %d", lineNumber);
                }
                return true;
            } else {
                if (logger != null) {
                    logger.warn("No executable code found at line %d", lineNumber);
                }
                return false;
            }
        } catch (AbsentInformationException e) {
            if (logger != null) {
                logger.error("No line information available", e);
            }
            return false;
        }
    }
    private Location findMainMethodFirstLine(ReferenceType type) {
        try {
            List<Method> methods = type.methodsByName("main");
            for (Method method : methods) {
                if (method.isStatic() && method.signature().equals("([Ljava/lang/String;)V")) {
                    List<Location> locations = method.allLineLocations();
                    if (!locations.isEmpty()) {
                        return locations.get(0);
                    }
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Error finding main method", e);
            }
        }
        return null;
    }
}
