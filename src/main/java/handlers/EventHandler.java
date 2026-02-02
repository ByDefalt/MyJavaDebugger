package handlers;

import com.sun.jdi.event.Event;
import models.DebuggerState;

/**
 * Interface pour les gestionnaires d'événements JDI (Open/Closed Principle)
 * Chaque type d'événement a son propre handler, permettant d'ajouter de nouveaux
 * handlers sans modifier le code existant.
 */
public interface EventHandler<T extends Event> {

    /**
     * Vérifie si ce handler peut traiter l'événement donné
     */
    boolean canHandle(Event event);

    /**
     * Traite l'événement
     * @param event L'événement à traiter
     * @param context Le contexte du debugger
     * @return Le résultat du traitement
     */
    EventHandlerResult handle(T event, DebuggerState context) throws Exception;
}
