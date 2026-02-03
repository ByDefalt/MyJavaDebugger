package models;

import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.List;

public class DebugFrame {
    private StackFrame frame;
    private Location location;
    private List<Variable> temporaries;
    private ObjectReference receiver;

    private String displayName;
    private String sourceFile;
    private int lineNumber;

    public DebugFrame(StackFrame frame) throws IncompatibleThreadStateException {
        this.frame = frame;
        this.location = frame.location();
        this.temporaries = new ArrayList<>();
        this.receiver = frame.thisObject();
        loadTemporaries();
    }

    public DebugFrame(String displayName, String sourceFile, int lineNumber) {
        this.displayName = displayName;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.temporaries = new ArrayList<>();
    }

    private void loadTemporaries() throws IncompatibleThreadStateException {
        try {
            for (LocalVariable lv : frame.visibleVariables()) {
                Value val = frame.getValue(lv);
                temporaries.add(new Variable(lv.name(), lv.typeName(), val));
            }
        } catch (AbsentInformationException e) {
            
        }
    }

    public StackFrame getFrame() { return frame; }
    public Location getLocation() { return location; }
    public List<Variable> getTemporaries() { return temporaries; }
    public ObjectReference getReceiver() { return receiver; }

    @Override
    public String toString() {
        if (location != null) {
            return location.declaringType().name() + "." +
                    location.method().name() + "() ligne " + location.lineNumber();
        }
        return displayName + " ligne " + lineNumber;
    }

    public String getDisplayName() { return displayName; }
    public String getSourceFile() { return sourceFile; }
    public int getLineNumber() { return lineNumber; }
}
