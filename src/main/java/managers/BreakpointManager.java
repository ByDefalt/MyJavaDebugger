package managers;

import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import models.Breakpoint;
import models.DebuggerState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gestionnaire centralisé des breakpoints (Single Responsibility Principle)
 * Encapsule toute la logique liée à la gestion des breakpoints
 */
public class BreakpointManager {

    private final DebuggerState state;

    public BreakpointManager(DebuggerState state) {
        this.state = state;
    }

    /**
     * Crée un breakpoint normal
     */
    public Optional<Breakpoint> createBreakpoint(String fileName, int lineNumber) {
        return createBreakpoint(fileName, lineNumber, Breakpoint.BreakpointType.NORMAL, 0);
    }

    /**
     * Crée un breakpoint one-shot
     */
    public Optional<Breakpoint> createBreakpointOnce(String fileName, int lineNumber) {
        return createBreakpoint(fileName, lineNumber, Breakpoint.BreakpointType.ONCE, 0);
    }

    /**
     * Crée un breakpoint qui s'active après N passages
     */
    public Optional<Breakpoint> createBreakpointOnCount(String fileName, int lineNumber, int count) {
        return createBreakpoint(fileName, lineNumber, Breakpoint.BreakpointType.ON_COUNT, count);
    }

    /**
     * Crée un breakpoint avec type et count spécifiés
     */
    public Optional<Breakpoint> createBreakpoint(String fileName, int lineNumber,
            Breakpoint.BreakpointType type, int count) {

        String normalizedFileName = normalizeFileName(fileName);
        String key = normalizedFileName + ":" + lineNumber;

        // En mode replay, créer un breakpoint logique
        if (state.isReplayMode()) {
            Breakpoint bp = new Breakpoint(normalizedFileName, lineNumber, null, type, count);
            state.getBreakpoints().put(key, bp);
            return Optional.of(bp);
        }

        // Mode normal : créer un vrai BreakpointRequest
        VirtualMachine vm = state.getVm();
        for (ReferenceType refType : vm.allClasses()) {
            try {
                String sourceName = refType.sourceName();
                if (sourceName.equals(normalizedFileName) || sourceName.equals(fileName)) {
                    List<Location> locs = refType.locationsOfLine(lineNumber);
                    if (!locs.isEmpty()) {
                        Location loc = locs.get(0);
                        BreakpointRequest req = vm.eventRequestManager().createBreakpointRequest(loc);
                        req.enable();

                        Breakpoint bp = new Breakpoint(normalizedFileName, lineNumber, req, type, count);
                        state.getBreakpoints().put(key, bp);
                        return Optional.of(bp);
                    }
                }
            } catch (Exception e) {
                // Ignorer les erreurs de sources non disponibles
            }
        }

        return Optional.empty();
    }

    /**
     * Supprime un breakpoint
     */
    public boolean removeBreakpoint(String fileName, int lineNumber) {
        String key = normalizeFileName(fileName) + ":" + lineNumber;
        Breakpoint bp = state.getBreakpoints().remove(key);

        if (bp != null && bp.getRequest() != null) {
            bp.getRequest().disable();
            state.getVm().eventRequestManager().deleteEventRequest(bp.getRequest());
            return true;
        }

        return bp != null;
    }

    /**
     * Vérifie si un breakpoint existe à une position donnée
     */
    public Optional<Breakpoint> getBreakpoint(String sourceFile, int lineNumber) {
        // Essayer avec et sans extension .java
        String keyWithExtension = normalizeFileName(sourceFile) + ":" + lineNumber;
        String keyWithoutExtension = sourceFile.replace(".java", "") + ":" + lineNumber;

        Breakpoint bp = state.getBreakpoints().get(keyWithExtension);
        if (bp == null) {
            bp = state.getBreakpoints().get(keyWithoutExtension);
        }

        return Optional.ofNullable(bp);
    }

    /**
     * Retourne tous les breakpoints
     */
    public Map<String, Breakpoint> getAllBreakpoints() {
        return state.getBreakpoints();
    }

    /**
     * Supprime tous les breakpoints
     */
    public void clearAllBreakpoints() {
        for (Breakpoint bp : state.getBreakpoints().values()) {
            if (bp.getRequest() != null) {
                bp.getRequest().disable();
                state.getVm().eventRequestManager().deleteEventRequest(bp.getRequest());
            }
        }
        state.getBreakpoints().clear();
    }

    /**
     * Normalise le nom de fichier (ajoute .java si absent)
     */
    private String normalizeFileName(String fileName) {
        if (!fileName.endsWith(".java")) {
            return fileName + ".java";
        }
        return fileName;
    }
}
