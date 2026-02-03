package gui;
import gui.components.SourceCodePanel;
import models.DebugFrame;
import models.ExecutionSnapshot;
import com.sun.jdi.Location;
import com.sun.jdi.AbsentInformationException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
/**
 * Responsible for loading source code files from various sources.
 * Centralizes the logic for finding and loading source files with fallback paths.
 * Follows Single Responsibility Principle.
 */
public class SourceFileLoader {
    private final SourceCodePanel sourceCodePanel;
    private final Consumer<String> onWarning;
    public SourceFileLoader(SourceCodePanel sourceCodePanel, Consumer<String> onWarning) {
        this.sourceCodePanel = sourceCodePanel;
        this.onWarning = onWarning;
    }
    public boolean loadFromLocation(Location loc) {
        try {
            String sourceName = loc.sourceName();
            String className = loc.declaringType().name();
            String packagePath = extractPackagePath(className);
            if (tryLoadSource(sourceName, packagePath)) {
                sourceCodePanel.setCurrentLine(loc.lineNumber());
                return true;
            }
            onWarning.accept("[WARN] Source file not found: " + sourceName);
            return false;
        } catch (AbsentInformationException e) {
            onWarning.accept("[WARN] No source information available");
            return false;
        }
    }
    public boolean loadFromFrame(DebugFrame frame) {
        String sourceName = frame.getSourceFile();
        String displayName = frame.getDisplayName();
        String packagePath = extractPackagePathFromDisplayName(displayName);
        if (tryLoadSource(sourceName, packagePath)) {
            sourceCodePanel.setCurrentLine(frame.getLineNumber());
            return true;
        }
        onWarning.accept("[WARN] Error loading source from frame");
        return false;
    }
    public boolean loadFromSnapshot(ExecutionSnapshot snapshot, int lineNumber) {
        String sourceName = snapshot.getSourceFile();
        String className = snapshot.getClassName();
        String packagePath = extractPackagePath(className);
        if (tryLoadSource(sourceName, packagePath)) {
            sourceCodePanel.setCurrentLine(lineNumber);
            return true;
        }
        return false;
    }
    private boolean tryLoadSource(String sourceName, String packagePath) {
        String[] possiblePaths = buildPossiblePaths(packagePath, sourceName);
        for (String path : possiblePaths) {
            if (Files.exists(Paths.get(path))) {
                try {
                    List<String> lines = Files.readAllLines(Paths.get(path));
                    sourceCodePanel.setSourceLines(lines);
                    return true;
                } catch (Exception e) {
                    onWarning.accept("[WARN] Error reading file: " + path);
                }
            }
        }
        return false;
    }
    private String[] buildPossiblePaths(String packagePath, String sourceName) {
        return new String[] {
            "src/main/java/" + packagePath + sourceName,
            "src/main/java/dbg/" + sourceName,
            "src/main/java/" + sourceName,
            "src/" + packagePath + sourceName,
            sourceName
        };
    }
    private String extractPackagePath(String className) {
        if (className == null || !className.contains(".")) {
            return "";
        }
        return className.substring(0, className.lastIndexOf('.')).replace('.', '/') + "/";
    }
    private String extractPackagePathFromDisplayName(String displayName) {
        if (displayName == null || !displayName.contains(".")) {
            return "";
        }
        String className = displayName.substring(0, displayName.lastIndexOf('.'));
        if (className.contains(".")) {
            return className.substring(0, className.lastIndexOf('.')).replace('.', '/') + "/";
        }
        return "";
    }
}
