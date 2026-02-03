package gui;
import dbg.JDISimpleDebuggee;
public class JDISimpleDebuggerGUI {
    public static void main(String[] args) {
        ScriptableDebuggerGUI debugger = new ScriptableDebuggerGUI();
        debugger.attachTo(JDISimpleDebuggee.class);
    }
}