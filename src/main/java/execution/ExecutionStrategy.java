package execution;

import commands.CommandResult;
import models.DebuggerState;

/**
 * Interface pour les stratégies d'exécution (Strategy Pattern + OCP)
 * Permet de séparer la logique live vs replay sans if/else dans les commandes
 */
public interface ExecutionStrategy {

    /**
     * Exécute un step
     */
    CommandResult step(DebuggerState state) throws Exception;

    /**
     * Exécute un step-over
     */
    CommandResult stepOver(DebuggerState state) throws Exception;

    /**
     * Continue l'exécution
     */
    CommandResult continueExecution(DebuggerState state) throws Exception;

    /**
     * Vérifie si cette stratégie est applicable
     */
    boolean isApplicable(DebuggerState state);
}
