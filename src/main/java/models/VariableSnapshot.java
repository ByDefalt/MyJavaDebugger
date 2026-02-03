package models;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
public class VariableSnapshot {
    private final String name;
    private final String type;
    private final String value;
    private final String methodName;
    private final String className;
    private final int frameIndex;
    private final int slot;
    private final List<VariableSnapshot> children;
    public VariableSnapshot(String name, String type, String value,
                           String methodName, String className, int frameIndex, int slot) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.methodName = methodName;
        this.className = className;
        this.frameIndex = frameIndex;
        this.slot = slot;
        this.children = new ArrayList<>();
    }
    public void addChild(VariableSnapshot child) {
        children.add(child);
    }
    public List<VariableSnapshot> getChildren() {
        return Collections.unmodifiableList(children);
    }
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    public String getUniqueId() {
        return String.format("%s.%s#%d:%s@%d", className, methodName, frameIndex, name, slot);
    }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getValue() { return value; }
    public String getMethodName() { return methodName; }
    public String getClassName() { return className; }
    public int getFrameIndex() { return frameIndex; }
    public int getSlot() { return slot; }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableSnapshot that = (VariableSnapshot) o;
        return frameIndex == that.frameIndex &&
               slot == that.slot &&
               Objects.equals(name, that.name) &&
               Objects.equals(methodName, that.methodName) &&
               Objects.equals(className, that.className);
    }
    @Override
    public int hashCode() {
        return Objects.hash(name, methodName, className, frameIndex, slot);
    }
    @Override
    public String toString() {
        return String.format("%s (%s) = %s [%s.%s #%d]",
                name, type, value, className, methodName, frameIndex);
    }
}
