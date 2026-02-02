package gui;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import commands.*;
import models.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

public class ScriptableDebuggerGUI implements DebuggerGUI.DebuggerCallback {
    private Class debugClass;
    private VirtualMachine vm;
    private DebuggerState state;
    private DebuggerGUI gui;
    private volatile boolean isRunning = true;
    private boolean shouldContinue = false;

    public void attachTo(Class debuggeeClass) {
        this.debugClass = debuggeeClass;

        SwingUtilities.invokeLater(() -> {
            try {
                gui = new DebuggerGUI();
                gui.setCallback(this);
                gui.setVisible(true);
                gui.appendOutput("🚀 Debugging " + debugClass.getSimpleName() + "...\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            vm = connectAndLaunchVM();
            state = new DebuggerState(vm);

            captureTargetOutput();

            enableClassPrepareRequest(vm);
            startDebuggerLoop();

        } catch (Exception e) {
            e.printStackTrace();
            if (gui != null) gui.appendOutput("Erreur de connexion : " + e.getMessage());
        }
    }

    private void captureTargetOutput() {
        Process process = vm.process();
        if (process == null) return;

        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while (isRunning && (line = reader.readLine()) != null) {
                    String finalLine = line;
                    try {
                        gui.appendOutput(finalLine + "\n");
                    }catch (Exception ignored){

                    }
                }
            } catch (IOException e) {
                if (isRunning) gui.appendOutput("Erreur de lecture console: " + e.getMessage() + "\n");
            }
        });

        readerThread.setDaemon(true);
        readerThread.start();
    }

    private VirtualMachine connectAndLaunchVM() throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();
        args.get("main").setValue(debugClass.getName());
        args.get("options").setValue("-cp " + System.getProperty("java.class.path"));
        return connector.launch(args);
    }

    private void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
        r.addClassFilter(debugClass.getName());
        r.enable();
    }

    private void startDebuggerLoop() throws Exception {
        while (isRunning) {
            EventSet eventSet = vm.eventQueue().remove();
            for (Event event : eventSet) {
                if (event instanceof VMDisconnectEvent) {
                    gui.appendOutput("\nProcess finished.");
                    gui.enableControls(false);
                    return;
                }
                if (event instanceof ClassPrepareEvent) {
                    setInitialBreakpoint();
                }
                if (event instanceof BreakpointEvent || event instanceof StepEvent) {
                    LocatableEvent le = (LocatableEvent) event;
                    if (event instanceof StepEvent) vm.eventRequestManager().deleteEventRequest(event.request());
                    handleStop(le.location(), le.thread());
                }
            }
            vm.resume();
        }
    }

    private void setInitialBreakpoint() {
        for (ReferenceType type : vm.allClasses()) {
            if (type.name().equals(debugClass.getName())) {
                try {
                    List<Location> locs = type.locationsOfLine(6);
                    if (!locs.isEmpty()) {
                        vm.eventRequestManager().createBreakpointRequest(locs.get(0)).enable();
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void handleStop(Location loc, ThreadReference thread) throws IncompatibleThreadStateException {
        state.updateContext(thread);
        SwingUtilities.invokeLater(() -> {
            gui.updateDebuggerState(state, loc, thread);
            gui.enableControls(true);
        });
        waitForCommand();
    }

    private synchronized void waitForCommand() {
        shouldContinue = false;
        while (!shouldContinue && isRunning) {
            try { wait(100); } catch (InterruptedException e) { break; }
        }
    }

    @Override
    public synchronized void executeCommand(Command cmd) throws Exception {
        cmd.execute(state);
        shouldContinue = true;
        notifyAll();
    }

    @Override
    public void placeBreakpoint(String file, int line) throws Exception {
        new BreakCommand(file, line).execute(state);
        gui.appendOutput("Breakpoint set: " + file + ":" + line + "\n");
    }

    @Override
    public void stop() {
        isRunning = false;
        if (vm != null) vm.exit(0);
    }
}