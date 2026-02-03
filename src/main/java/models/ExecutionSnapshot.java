package models;

import com.sun.jdi.*;
import java.util.*;

public class ExecutionSnapshot {
    private final int stepNumber;
    private final String sourceFile;
    private final int lineNumber;
    private final String methodName;
    private final String className;
    private final List<StackFrameSnapshot> stackFrames;
    private final Map<String, String> localVariables;
    private final List<VariableSnapshot> variableSnapshots;
    private final String receiverInfo;
    private final long timestamp;

    public ExecutionSnapshot(int stepNumber, ThreadReference thread) throws IncompatibleThreadStateException, AbsentInformationException {
        this.stepNumber = stepNumber;
        this.timestamp = System.currentTimeMillis();

        StackFrame frame = thread.frame(0);
        Location location = frame.location();

        this.sourceFile = location.sourceName();
        this.lineNumber = location.lineNumber();
        this.methodName = location.method().name();
        this.className = location.declaringType().name();

        this.stackFrames = new ArrayList<>();
        this.variableSnapshots = new ArrayList<>();

        for (int i = 0; i < thread.frameCount(); i++) {
            StackFrame sf = thread.frame(i);
            stackFrames.add(new StackFrameSnapshot(sf, i));
            captureVariables(sf, i);
        }

        this.localVariables = new HashMap<>();
        try {
            Map<LocalVariable, Value> vars = frame.getValues(frame.visibleVariables());
            for (Map.Entry<LocalVariable, Value> entry : vars.entrySet()) {
                String name = entry.getKey().name();
                String value = valueToString(entry.getValue());
                localVariables.put(name, value);
            }
        } catch (AbsentInformationException e) {
            
        }

        ObjectReference thisObj = frame.thisObject();
        if (thisObj != null) {
            this.receiverInfo = thisObj.referenceType().name() + "@" + thisObj.uniqueID();
        } else {
            this.receiverInfo = "static context";
        }
    }

    private void captureVariables(StackFrame frame, int frameIndex) {
        try {
            Location loc = frame.location();
            String framClassName = loc.declaringType().name();
            String framMethodName = loc.method().name();

            List<LocalVariable> vars = frame.visibleVariables();
            for (int i = 0; i < vars.size(); i++) {
                LocalVariable lv = vars.get(i);
                Value val = frame.getValue(lv);
                VariableSnapshot vs = new VariableSnapshot(
                    lv.name(),
                    lv.typeName(),
                    valueToString(val),
                    framMethodName,
                    framClassName,
                    frameIndex,
                    i
                );
                variableSnapshots.add(vs);
            }
        } catch (AbsentInformationException e) {
        }
    }

    private String valueToString(Value value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof StringReference) {
            return "\"" + ((StringReference) value).value() + "\"";
        }
        if (value instanceof ObjectReference) {
            ObjectReference obj = (ObjectReference) value;
            return obj.referenceType().name() + "@" + obj.uniqueID();
        }
        return value.toString();
    }

    public int getStepNumber() { return stepNumber; }
    public String getSourceFile() { return sourceFile; }
    public int getLineNumber() { return lineNumber; }
    public String getMethodName() { return methodName; }
    public String getClassName() { return className; }
    public List<StackFrameSnapshot> getStackFrames() { return stackFrames; }
    public Map<String, String> getLocalVariables() { return localVariables; }
    public List<VariableSnapshot> getVariableSnapshots() { return variableSnapshots; }
    public String getReceiverInfo() { return receiverInfo; }
    public long getTimestamp() { return timestamp; }

    public List<VariableSnapshot> getVariablesForFrame(int frameIndex) {
        List<VariableSnapshot> result = new ArrayList<>();
        for (VariableSnapshot vs : variableSnapshots) {
            if (vs.getFrameIndex() == frameIndex) {
                result.add(vs);
            }
        }
        return result;
    }

    public VariableSnapshot getVariableById(String uniqueId) {
        for (VariableSnapshot vs : variableSnapshots) {
            if (vs.getUniqueId().equals(uniqueId)) {
                return vs;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("Step #%d: %s:%d - %s.%s() [%d frames]",
                stepNumber, sourceFile, lineNumber, className, methodName, stackFrames.size());
    }

    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Step #").append(stepNumber).append(" ===\n");
        sb.append("Location: ").append(sourceFile).append(":").append(lineNumber).append("\n");
        sb.append("Method: ").append(className).append(".").append(methodName).append("()\n");
        sb.append("Receiver: ").append(receiverInfo).append("\n");
        sb.append("\nLocal Variables:\n");
        if (localVariables.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (Map.Entry<String, String> entry : localVariables.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
        }
        sb.append("\nCall Stack:\n");
        for (StackFrameSnapshot frame : stackFrames) {
            sb.append("  ").append(frame).append("\n");
        }
        return sb.toString();
    }

    public static class StackFrameSnapshot {
        private final int frameIndex;
        private final String methodName;
        private final String className;
        private final String sourceFile;
        private final int lineNumber;

        public StackFrameSnapshot(StackFrame frame, int index) throws AbsentInformationException {
            this.frameIndex = index;
            Location loc = frame.location();
            this.methodName = loc.method().name();
            this.className = loc.declaringType().name();
            this.sourceFile = loc.sourceName();
            this.lineNumber = loc.lineNumber();
        }

        @Override
        public String toString() {
            return String.format("#%d %s.%s() at %s:%d",
                    frameIndex, className, methodName, sourceFile, lineNumber);
        }

        public int getFrameIndex() { return frameIndex; }
        public String getMethodName() { return methodName; }
        public String getClassName() { return className; }
        public String getSourceFile() { return sourceFile; }
        public int getLineNumber() { return lineNumber; }
    }
}
