package models;

import com.sun.jdi.*;
import java.util.*;

/**
 * Représente un instantané complet de l'état d'exécution à un moment donné
 */
public class ExecutionSnapshot {
    private final int stepNumber;
    private final String sourceFile;
    private final int lineNumber;
    private final String methodName;
    private final String className;
    private final List<StackFrameSnapshot> stackFrames;
    private final Map<String, String> localVariables;
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

        // Capturer la pile d'appels
        this.stackFrames = new ArrayList<>();
        for (int i = 0; i < thread.frameCount(); i++) {
            stackFrames.add(new StackFrameSnapshot(thread.frame(i), i));
        }

        // Capturer les variables locales
        this.localVariables = new HashMap<>();
        try {
            Map<LocalVariable, Value> vars = frame.getValues(frame.visibleVariables());
            for (Map.Entry<LocalVariable, Value> entry : vars.entrySet()) {
                String name = entry.getKey().name();
                String value = valueToString(entry.getValue());
                localVariables.put(name, value);
            }
        } catch (AbsentInformationException e) {
            // Pas d'informations de variables disponibles
        }

        // Capturer le receiver (this)
        ObjectReference thisObj = frame.thisObject();
        if (thisObj != null) {
            this.receiverInfo = thisObj.referenceType().name() + "@" + thisObj.uniqueID();
        } else {
            this.receiverInfo = "static context";
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
    public String getReceiverInfo() { return receiverInfo; }
    public long getTimestamp() { return timestamp; }

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

    /**
     * Instantané d'une frame de la pile d'appels
     */
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
