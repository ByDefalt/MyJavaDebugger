package commands;


import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import models.DebuggerState;
import models.Variable;
import models.DebugFrame;

class PrintVarCommand implements Command {
    private String varName;

    public PrintVarCommand(String varName) {
        this.varName = varName;
    }

    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        DebugFrame frame = state.getContext().getCurrentFrame();
        if (frame == null) {
            return CommandResult.error("No current frame");
        }

        // Chercher dans les variables locales
        for (Variable v : frame.getTemporaries()) {
            if (v.getName().equals(varName)) {
                return CommandResult.success(v);
            }
        }

        // Chercher dans les variables d'instance
        ObjectReference receiver = frame.getReceiver();
        if (receiver != null) {
            for (Field field : receiver.referenceType().allFields()) {
                if (field.name().equals(varName)) {
                    Value val = receiver.getValue(field);
                    return CommandResult.success(
                            new Variable(field.name(), field.typeName(), val)
                    );
                }
            }
        }

        return CommandResult.error("Variable not found: " + varName);
    }
}
