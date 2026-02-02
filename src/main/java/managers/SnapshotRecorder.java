package managers;

import com.sun.jdi.ThreadReference;
import models.DebuggerState;
import models.ExecutionSnapshot;

/**
 * Gestionnaire d'enregistrement des snapshots (Single Responsibility Principle)
 * Responsabilité unique : enregistrer les états d'exécution
 */
public class SnapshotRecorder {

    private final DebuggerState state;

    public SnapshotRecorder(DebuggerState state) {
        this.state = state;
    }

    /**
     * Enregistre un snapshot de l'état d'exécution actuel
     */
    public void recordSnapshot(ThreadReference thread) {
        try {
            int stepNumber = state.getExecutionHistory().size();
            ExecutionSnapshot snapshot = new ExecutionSnapshot(stepNumber, thread);
            state.getExecutionHistory().addSnapshot(snapshot);
        } catch (Exception e) {
            // Log silencieux - ne pas interrompre l'enregistrement
        }
    }

    /**
     * Retourne le nombre de steps enregistrés
     */
    public int getStepCount() {
        return state.getExecutionHistory().size();
    }

    /**
     * Vérifie si on doit afficher une progression
     */
    public boolean shouldLogProgress() {
        int count = getStepCount();
        return count > 0 && count % 100 == 0;
    }
}
