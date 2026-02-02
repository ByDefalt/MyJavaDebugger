package commands;

import models.DebuggerState;

/**
 * Commande pour afficher l'aide
 */
public class HelpCommand implements Command {

    private final CommandInterpreter interpreter;
    private final String commandName;

    /**
     * Constructeur pour l'aide générale
     */
    public HelpCommand(CommandInterpreter interpreter) {
        this(interpreter, null);
    }

    /**
     * Constructeur pour l'aide d'une commande spécifique
     */
    public HelpCommand(CommandInterpreter interpreter, String commandName) {
        this.interpreter = interpreter;
        this.commandName = commandName;
    }

    @Override
    public CommandResult execute(DebuggerState state) {
        if (commandName != null && !commandName.isEmpty()) {
            // Aide pour une commande spécifique
            String help = interpreter.getHelp(commandName);
            return CommandResult.success(commandName + ": " + help, null);
        } else {
            // Aide générale
            return CommandResult.success(interpreter.getFullHelp(), null);
        }
    }
}
