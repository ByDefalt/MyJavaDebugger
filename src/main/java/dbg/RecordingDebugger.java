package dbg;

import io.ConsoleLogger;
import io.Logger;

public class RecordingDebugger {
    public static void main(String[] args) throws Exception {
        Logger log = new ConsoleLogger(Logger.Level.INFO);

        log.info("=== Recording Debugger ===");
        log.info("This will execute the entire program in step-in mode and record all states.");
        log.info("After execution, you can navigate through the recorded execution history.");

        // Créer le debugger en mode auto-record
        ScriptableDebugger debuggerInstance = new ScriptableDebugger(true);
        debuggerInstance.attachTo(JDISimpleDebuggee.class);
    }
}
