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
    private final String outputText;
    public ExecutionSnapshot(int stepNumber, ThreadReference thread) throws IncompatibleThreadStateException, AbsentInformationException {
        this(stepNumber, thread, "");
    }

    public ExecutionSnapshot(int stepNumber, ThreadReference thread, String outputText) throws IncompatibleThreadStateException, AbsentInformationException {
        this.outputText = outputText;
        this.stepNumber = stepNumber;
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
                VariableSnapshot vs = createVariableSnapshot(
                    lv.name(),
                    lv.typeName(),
                    val,
                    framMethodName,
                    framClassName,
                    frameIndex,
                    i,
                    0
                );
                variableSnapshots.add(vs);
            }
        } catch (AbsentInformationException e) {
        }
    }
    private static final int MAX_DEPTH = 3;
    private static final int MAX_CHILDREN = 50;
    private VariableSnapshot createVariableSnapshot(String name, String type, Value value,
            String methodName, String className, int frameIndex, int slot, int depth) {
        VariableSnapshot vs = new VariableSnapshot(
            name, type, valueToString(value), methodName, className, frameIndex, slot
        );
        if (depth >= MAX_DEPTH || value == null) {
            return vs;
        }
        if (value instanceof ArrayReference) {
            ArrayReference array = (ArrayReference) value;
            int count = Math.min(array.length(), MAX_CHILDREN);
            for (int i = 0; i < count; i++) {
                Value elementValue = array.getValue(i);
                String elementType = elementValue != null ? elementValue.type().name() : "null";
                VariableSnapshot child = createVariableSnapshot(
                    "[" + i + "]", elementType, elementValue,
                    methodName, className, frameIndex, slot, depth + 1
                );
                vs.addChild(child);
            }
            if (array.length() > MAX_CHILDREN) {
                vs.addChild(new VariableSnapshot(
                    "...", "more", "(" + (array.length() - MAX_CHILDREN) + " more elements)",
                    methodName, className, frameIndex, slot
                ));
            }
        } else if (value instanceof ObjectReference && !(value instanceof StringReference)) {
            ObjectReference obj = (ObjectReference) value;
            try {
                ReferenceType refType = obj.referenceType();
                List<Field> fields = refType.allFields();
                int count = 0;
                for (Field field : fields) {
                    if (count >= MAX_CHILDREN) {
                        vs.addChild(new VariableSnapshot(
                            "...", "more", "(" + (fields.size() - MAX_CHILDREN) + " more fields)",
                            methodName, className, frameIndex, slot
                        ));
                        break;
                    }
                    try {
                        Value fieldValue = obj.getValue(field);
                        String fieldType = field.typeName();
                        VariableSnapshot child = createVariableSnapshot(
                            field.name(), fieldType, fieldValue,
                            methodName, className, frameIndex, slot, depth + 1
                        );
                        vs.addChild(child);
                        count++;
                    } catch (Exception e) {
                        vs.addChild(new VariableSnapshot(
                            field.name(), field.typeName(), "<inaccessible>",
                            methodName, className, frameIndex, slot
                        ));
                        count++;
                    }
                }
            } catch (Exception e) {
            }
        }
        return vs;
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
    public String getOutputText() { return outputText; }
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
        public StackFrameSnapshot(StackFrame frame, int index) {
            this.frameIndex = index;
            Location loc = frame.location();
            this.methodName = loc.method().name();
            this.className = loc.declaringType().name();
            this.lineNumber = loc.lineNumber();
            String source;
            try {
                source = loc.sourceName();
            } catch (AbsentInformationException e) {
                source = "Unknown";
            }
            this.sourceFile = source;
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
