package handlers;

/**
 * Résultat du traitement d'un événement par un handler
 */
public class EventHandlerResult {

    public enum Action {
        CONTINUE,           // Continuer l'exécution normalement
        WAIT_FOR_COMMAND,   // Attendre une commande utilisateur
        STOP,               // Arrêter le debugger
        ENTER_REPLAY_MODE   // Passer en mode replay
    }

    private final Action action;
    private final String message;
    private final Object data;

    public EventHandlerResult(Action action, String message, Object data) {
        this.action = action;
        this.message = message;
        this.data = data;
    }

    public static EventHandlerResult continueExecution() {
        return new EventHandlerResult(Action.CONTINUE, "", null);
    }

    public static EventHandlerResult continueExecution(String message) {
        return new EventHandlerResult(Action.CONTINUE, message, null);
    }

    public static EventHandlerResult waitForCommand(String message) {
        return new EventHandlerResult(Action.WAIT_FOR_COMMAND, message, null);
    }

    public static EventHandlerResult stop() {
        return new EventHandlerResult(Action.STOP, "", null);
    }

    public static EventHandlerResult stop(String message) {
        return new EventHandlerResult(Action.STOP, message, null);
    }

    public static EventHandlerResult enterReplayMode(String message) {
        return new EventHandlerResult(Action.ENTER_REPLAY_MODE, message, null);
    }

    public Action getAction() { return action; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
