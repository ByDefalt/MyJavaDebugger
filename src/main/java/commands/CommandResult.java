package commands;
import models.ExecutionSnapshot;
public class CommandResult {
    private boolean success;
    private String message;
    private Object data;
    public CommandResult(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    public static CommandResult success(Object data) {
        return new CommandResult(true, "", data);
    }
    public static CommandResult success(String message, Object data) {
        return new CommandResult(true, message, data);
    }
    public static CommandResult error(String message) {
        return new CommandResult(false, message, null);
    }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public boolean hasSnapshot() {
        return data instanceof ExecutionSnapshot;
    }
    public ExecutionSnapshot getSnapshot() {
        if (data instanceof ExecutionSnapshot) {
            return (ExecutionSnapshot) data;
        }
        return null;
    }
}
