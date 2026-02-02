package dbg;

/**
 * Debugger avec enregistrement automatique de tout le code en mode step-in
 * L'utilisateur peut ensuite naviguer dans l'historique d'exécution enregistré
 */
public class RecordingDebugger {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Recording Debugger ===");
        System.out.println("This will execute the entire program in step-in mode and record all states.");
        System.out.println("After execution, you can navigate through the recorded execution history.\n");

        // Créer le debugger en mode auto-record
        ScriptableDebugger debuggerInstance = new ScriptableDebugger(true);
        debuggerInstance.attachTo(JDISimpleDebuggee.class);
    }
}
