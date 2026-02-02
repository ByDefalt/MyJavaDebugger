package handlers;

import com.sun.jdi.event.Event;
import models.DebuggerState;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry pour les handlers d'événements (Open/Closed Principle)
 * Permet d'enregistrer et de dispatcher les événements aux handlers appropriés
 */
public class EventHandlerRegistry {

    private final List<EventHandler<?>> handlers = new ArrayList<>();

    /**
     * Enregistre un nouveau handler
     */
    public void register(EventHandler<?> handler) {
        handlers.add(handler);
    }

    /**
     * Dispatche un événement au handler approprié
     */
    @SuppressWarnings("unchecked")
    public EventHandlerResult dispatch(Event event, DebuggerState state) throws Exception {
        for (EventHandler<?> handler : handlers) {
            if (handler.canHandle(event)) {
                return ((EventHandler<Event>) handler).handle(event, state);
            }
        }
        // Aucun handler trouvé, continuer l'exécution
        return EventHandlerResult.continueExecution();
    }

    /**
     * Retourne le nombre de handlers enregistrés
     */
    public int size() {
        return handlers.size();
    }
}
