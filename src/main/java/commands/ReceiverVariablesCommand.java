package commands;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import models.DebugFrame;
import models.Variable;

import java.util.ArrayList;
import java.util.List;

class ReceiverVariablesCommand implements Command {
    @Override
    public CommandResult execute(DebuggerState state) throws Exception {
        DebugFrame frame = state.getContext().getCurrentFrame();
        if (frame == null) {
            return CommandResult.error("No current frame");
        }
        ObjectReference receiver = frame.getReceiver();
        if (receiver == null) {
            return CommandResult.error("No receiver");
        }

        List<Variable> variables = new ArrayList<>();
        ReferenceType type = receiver.referenceType();
        for (Field field : type.allFields()) {
            Value val = receiver.getValue(field);
            variables.add(new Variable(field.name(), field.typeName(), val));
        }
        return CommandResult.success(variables);
    }
}
