package dbg;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.Map;
import java.util.Scanner;

public class ScriptableDebugger {

    private Class debugClass;
    private VirtualMachine vm;

    public VirtualMachine connectAndLaunchVM() throws IOException, IllegalConnectorArgumentsException, VMStartException {
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
        arguments.get("main").setValue(debugClass.getName());
        arguments.get("options").setValue("-cp " + System.getProperty("java.class.path"));
        VirtualMachine vm = launchingConnector.launch(arguments);
        return vm;
    }

    public void attachTo(Class debuggeeClass) {
        this.debugClass = debuggeeClass;
        try {
            vm = connectAndLaunchVM();
            enableClassPrepareRequest(vm);
            startDebugger();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest r = vm.eventRequestManager().createClassPrepareRequest();
        r.addClassFilter(debugClass.getName());
        r.enable();
    }

    public void setBreakPoint(String className, int lineNumber) {
        for (ReferenceType t : vm.allClasses()) {
            if (t.name().equals(className)) {
                try {
                    Location loc = t.locationsOfLine(lineNumber).get(0);
                    BreakpointRequest req = vm.eventRequestManager().createBreakpointRequest(loc);
                    req.enable();
                    System.out.println("Breakpoint placé sur " + className + ":" + lineNumber);
                } catch (Exception e) {
                    System.out.println("ERREUR: Impossible de placer le breakpoint");
                }
            }
        }
    }

    public void askUserAndStep(ThreadReference thread) {
        System.out.print("Commande (step / continue) > ");
        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();
        if (input.equals("step")) {
            StepRequest sr = vm.eventRequestManager()
                    .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER);
            sr.addCountFilter(1);
            sr.enable();
        } else {
            System.out.println("Continuer jusqu’à la fin...");
        }
    }

    public void startDebugger() throws InterruptedException, AbsentInformationException {
        while (true) {
            EventSet eventSet = vm.eventQueue().remove();
            for (Event event : eventSet) {
                if (event instanceof VMDisconnectEvent) {
                    System.out.println("=== End of program ===");
                    printProcessOutput();
                    return;
                }
                if (event instanceof ClassPrepareEvent) {
                    System.out.println("ClassPrepareEvent reçu !");
                    setBreakPoint(debugClass.getName(), 6);
                }
                if (event instanceof BreakpointEvent) {
                    System.out.println("Breakpoint atteint !");
                    askUserAndStep(((BreakpointEvent) event).thread());
                }
                if (event instanceof StepEvent) {
                    StepEvent se = (StepEvent) event;
                    System.out.println("Step : "
                            + se.location().sourceName()
                            + ":" + se.location().lineNumber());
                    eventSet.virtualMachine().eventRequestManager().deleteEventRequest(event.request());
                    askUserAndStep(se.thread());
                }
            }
            vm.resume();
        }
    }

    public void printProcessOutput() {
        try {
            InputStreamReader reader = new InputStreamReader(vm.process().getInputStream());
            OutputStreamWriter writer = new OutputStreamWriter(System.out);
            reader.transferTo(writer);
            writer.flush();
        } catch (IOException e) {
            System.out.println("Erreur lecture output VM");
        }
    }
}
