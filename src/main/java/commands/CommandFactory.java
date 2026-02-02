package commands;

/**
 * Factory pour créer des commandes (Factory Pattern)
 * Utilisée par CommandInterpreter pour instancier les commandes à partir des arguments
 */
@FunctionalInterface
public interface CommandFactory {
    Command create(String[] args) throws Exception;
}
