package gui;

import dbg.JDISimpleDebuggee;

/**
 * Point d'entrée pour lancer le debugger avec interface graphique
 */
public class JDISimpleDebuggerGUI {
    public static void main(String[] args) throws Exception {
        ModernScriptableDebuggerGUI debuggerInstance = new ModernScriptableDebuggerGUI();
        debuggerInstance.attachTo(JDISimpleDebuggee.class);
    }
}