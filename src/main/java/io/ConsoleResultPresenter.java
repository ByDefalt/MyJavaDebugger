package io;

import com.sun.jdi.ObjectReference;
import commands.CommandResult;
import models.*;

import java.util.List;

/**
 * Implémentation console de ResultPresenter
 */
public class ConsoleResultPresenter implements ResultPresenter {

    @Override
    public void displayResult(CommandResult result) {
        if (!result.isSuccess()) {
            error(result.getMessage());
            return;
        }

        if (!result.getMessage().isEmpty()) {
            info(result.getMessage());
        }

        Object data = result.getData();
        if (data == null) {
            return;
        }

        displayData(data);
    }

    private void displayData(Object data) {
        if (data instanceof Variable) {
            System.out.println(data);
        } else if (data instanceof List) {
            displayList((List<?>) data);
        } else if (data instanceof DebugFrame) {
            System.out.println("Current frame: " + data);
        } else if (data instanceof CallStack) {
            System.out.println(data);
        } else if (data instanceof MethodInfo) {
            System.out.println("Method: " + data);
        } else if (data instanceof ObjectReference) {
            ObjectReference obj = (ObjectReference) data;
            System.out.println(obj.referenceType().name() + "@" + obj.uniqueID());
        } else if (data instanceof Breakpoint) {
            System.out.println("Breakpoint: " + data);
        } else if (data instanceof ExecutionHistory) {
            // Ne rien afficher - déjà affiché dans le message
        } else if (data instanceof ExecutionSnapshot) {
            // Ne rien afficher - déjà affiché dans le message
        } else {
            System.out.println(data);
        }
    }

    private void displayList(List<?> list) {
        if (list.isEmpty()) {
            System.out.println("(empty)");
        } else {
            for (Object item : list) {
                System.out.println("  " + item);
            }
        }
    }

    @Override
    public void info(String message) {
        System.out.println(message);
    }

    @Override
    public void error(String message) {
        System.err.println("ERROR: " + message);
    }

    @Override
    public void warn(String message) {
        System.out.println("WARN: " + message);
    }
}
