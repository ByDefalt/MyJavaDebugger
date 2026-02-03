package commands;

import models.DebuggerState;

public class HelpCommand implements Command {

    private final CommandInterpreter interpreter;
    private final String commandName;

    public HelpCommand(CommandInterpreter interpreter) {
        this(interpreter, null);
    }

    public HelpCommand(CommandInterpreter interpreter, String commandName) {
        this.interpreter = interpreter;
        this.commandName = commandName;
    }

    @Override
    public CommandResult execute(DebuggerState state) {
        if (commandName != null && !commandName.isEmpty()) {
            
            String help = interpreter.getHelp(commandName);
            return CommandResult.success(commandName + ": " + help, null);
        } else {
            
            return CommandResult.success(interpreter.getFullHelp(), null);
        }
    }
}
