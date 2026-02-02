package models;

import java.util.*;

/**
 * Historique complet de l'exécution enregistrée
 */
public class ExecutionHistory {
    private final List<ExecutionSnapshot> snapshots;
    private int currentIndex;
    private boolean recordingComplete;

    public ExecutionHistory() {
        this.snapshots = new ArrayList<>();
        this.currentIndex = -1;
        this.recordingComplete = false;
    }

    /**
     * Ajoute un nouveau snapshot à l'historique
     */
    public void addSnapshot(ExecutionSnapshot snapshot) {
        snapshots.add(snapshot);
        currentIndex = snapshots.size() - 1;
    }

    /**
     * Marque l'enregistrement comme terminé
     */
    public void completeRecording() {
        this.recordingComplete = true;
        this.currentIndex = 0; // Repositionner au début pour le replay
    }

    /**
     * Avance d'un pas dans l'historique
     */
    public boolean forward() {
        if (currentIndex < snapshots.size() - 1) {
            currentIndex++;
            return true;
        }
        return false;
    }

    /**
     * Recule d'un pas dans l'historique
     */
    public boolean back() {
        if (currentIndex > 0) {
            currentIndex--;
            return true;
        }
        return false;
    }

    /**
     * Va à un step spécifique
     */
    public boolean goToStep(int stepNumber) {
        if (stepNumber >= 0 && stepNumber < snapshots.size()) {
            currentIndex = stepNumber;
            return true;
        }
        return false;
    }

    /**
     * Retourne au début
     */
    public void goToStart() {
        currentIndex = 0;
    }

    /**
     * Va à la fin
     */
    public void goToEnd() {
        if (!snapshots.isEmpty()) {
            currentIndex = snapshots.size() - 1;
        }
    }

    /**
     * Obtient le snapshot courant
     */
    public ExecutionSnapshot getCurrentSnapshot() {
        if (currentIndex >= 0 && currentIndex < snapshots.size()) {
            return snapshots.get(currentIndex);
        }
        return null;
    }

    /**
     * Obtient un snapshot spécifique
     */
    public ExecutionSnapshot getSnapshot(int index) {
        if (index >= 0 && index < snapshots.size()) {
            return snapshots.get(index);
        }
        return null;
    }

    /**
     * Retourne tous les snapshots
     */
    public List<ExecutionSnapshot> getAllSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int size() {
        return snapshots.size();
    }

    public boolean isEmpty() {
        return snapshots.isEmpty();
    }

    public boolean isRecordingComplete() {
        return recordingComplete;
    }

    public boolean hasNext() {
        return currentIndex < snapshots.size() - 1;
    }

    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    @Override
    public String toString() {
        if (snapshots.isEmpty()) {
            return "No execution history recorded";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Execution History ===\n");
        sb.append("Total steps: ").append(snapshots.size()).append("\n");
        sb.append("Current position: ").append(currentIndex).append("\n");
        sb.append("Recording: ").append(recordingComplete ? "Complete" : "In progress").append("\n\n");

        // Afficher un aperçu des derniers steps autour de la position courante
        int start = Math.max(0, currentIndex - 5);
        int end = Math.min(snapshots.size(), currentIndex + 6);

        for (int i = start; i < end; i++) {
            String marker = (i == currentIndex) ? " >>> " : "     ";
            sb.append(marker).append(snapshots.get(i)).append("\n");
        }

        if (end < snapshots.size()) {
            sb.append("     ... (").append(snapshots.size() - end).append(" more steps)\n");
        }

        return sb.toString();
    }
}
