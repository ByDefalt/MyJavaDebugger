package dbg;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import models.DebuggerState;

import java.io.*;
import java.util.*;

public abstract class AbstractDebugger {

    protected Class<?> debugClass;
    protected VirtualMachine vm;
    protected DebuggerState state;

    protected volatile boolean isRunning = true;
    protected volatile boolean shouldContinue = false;
    protected final Object lock = new Object();

    public void attachTo(Class<?> debuggeeClass) {
        this.debugClass = debuggeeClass;

        try {
            
            initializeUI();

            vm = connectAndLaunchVM();
            state = new DebuggerState(vm);

            onInfo("[START] Debugging " + debugClass.getSimpleName() + "...");

            captureTargetOutput();
            onBeforeStart();
            enableClassPrepareRequest();
            startDebuggerLoop();

        } catch (Exception e) {
            onError("Connection error: " + e.getMessage());
            
        }
    }

    protected abstract void initializeUI();

    protected abstract void onBeforeStart();

    protected abstract void onInfo(String message);

    protected abstract void onError(String message);

    protected abstract void onOutput(String output);

    protected abstract void onVMDisconnect();

    protected abstract boolean onBreakpoint(Location loc, ThreadReference thread) throws Exception;

    protected abstract boolean onStep(Location loc, ThreadReference thread) throws Exception;

    protected boolean onMethodEntry(Location loc, ThreadReference thread) throws Exception {
        
        return false;
    }

    protected abstract void onClassPrepare(ReferenceType refType);

    protected VirtualMachine connectAndLaunchVM() throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();
        args.get("main").setValue(debugClass.getName());
        args.get("options").setValue("-cp " + System.getProperty("java.class.path"));
        return connector.launch(args);
    }

    protected void enableClassPrepareRequest() {
        ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
        r.addClassFilter(debugClass.getName());
        r.enable();
    }

    protected void captureTargetOutput() {
        Process process = vm.process();
        if (process == null) return;

        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while (isRunning && (line = reader.readLine()) != null) {
                    onOutput(line + "\n");
                }
            } catch (IOException e) {
                if (isRunning) {
                    onError("Output read error: " + e.getMessage());
                }
            }
        }, "Output-Capture-Thread");

        outputThread.setDaemon(true);
        outputThread.start();
    }

    protected void startDebuggerLoop() throws Exception {
        while (isRunning) {
            EventSet eventSet = vm.eventQueue().remove();
            boolean shouldResume = true;

            for (Event event : eventSet) {
                boolean needsWait = handleEvent(event);
                if (needsWait) {
                    shouldResume = false;
                }
            }

            if (shouldResume && isRunning) {
                vm.resume();
            }
        }
    }

    protected boolean handleEvent(Event event) throws Exception {
        if (event instanceof VMDisconnectEvent) {
            onVMDisconnect();
            isRunning = false;
            return false;
        } else if (event instanceof ClassPrepareEvent) {
            ClassPrepareEvent cpe = (ClassPrepareEvent) event;
            onClassPrepare(cpe.referenceType());
            return false;
        } else if (event instanceof BreakpointEvent) {
            BreakpointEvent be = (BreakpointEvent) event;
            return onBreakpoint(be.location(), be.thread());
        } else if (event instanceof StepEvent) {
            StepEvent se = (StepEvent) event;
            vm.eventRequestManager().deleteEventRequest(se.request());
            return onStep(se.location(), se.thread());
        } else if (event instanceof MethodEntryEvent) {
            MethodEntryEvent me = (MethodEntryEvent) event;
            return onMethodEntry(me.location(), me.thread());
        }
        return false;
    }

    protected void waitForUserCommand() {
        synchronized (lock) {
            shouldContinue = false;
            while (!shouldContinue && isRunning) {
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    protected void signalContinue() {
        synchronized (lock) {
            shouldContinue = true;
            lock.notifyAll();
        }
    }

    protected void resumeVM() {
        if (vm != null) {
            vm.resume();
        }
    }

    public void stop() {
        isRunning = false;
        if (vm != null) {
            try {
                vm.exit(0);
            } catch (Exception ignored) {}
        }
    }

    protected void printProcessOutput() {
        try {
            Process process = vm.process();
            if (process != null) {
                InputStreamReader reader = new InputStreamReader(process.getInputStream());
                BufferedReader buffered = new BufferedReader(reader);
                String line;
                while ((line = buffered.readLine()) != null) {
                    onOutput(line + "\n");
                }
            }
        } catch (IOException e) {
            onError("Error reading VM output: " + e.getMessage());
        }
    }

    public DebuggerState getState() { return state; }
    public VirtualMachine getVm() { return vm; }
    public Class<?> getDebugClass() { return debugClass; }
    public boolean isRunning() { return isRunning; }
}
