package gui;

import dbg.JDISimpleDebuggee;

/**
 * Point d'entrée pour lancer le debugger avec interface graphique
 */
public class JDISimpleDebuggerGUI {
    public static void main(String[] args) {
        ScriptableDebuggerGUI debugger = new ScriptableDebuggerGUI();
        debugger.attachTo(JDISimpleDebuggee.class);
    }
}