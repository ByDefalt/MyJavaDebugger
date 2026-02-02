package gui;

import dbg.JDISimpleDebuggee;

public class JDISimpleDebuggerGUI {
    public static void main(String[] args) throws Exception {
        ScriptableDebuggerGUI debuggerInstance = new ScriptableDebuggerGUI();
        debuggerInstance.attachTo(JDISimpleDebuggee.class);
    }
}