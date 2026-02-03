package io;

import com.sun.jdi.ObjectReference;
import commands.CommandResult;
import models.*;

import java.util.List;

public class ConsoleResultPresenter implements ResultPresenter {

    private final Logger logger;

    public ConsoleResultPresenter() {
        this.logger = new ConsoleLogger(Logger.Level.INFO);
    }

    public ConsoleResultPresenter(Logger logger) {
        this.logger = logger;
    }

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
            logger.info(data.toString());
        } else if (data instanceof List) {
            displayList((List<?>) data);
        } else if (data instanceof DebugFrame) {
            logger.info("Current frame: " + data);
        } else if (data instanceof CallStack) {
            logger.info(data.toString());
        } else if (data instanceof MethodInfo) {
            logger.info("Method: " + data);
        } else if (data instanceof ObjectReference) {
            ObjectReference obj = (ObjectReference) data;
            logger.info(obj.referenceType().name() + "@" + obj.uniqueID());
        } else if (data instanceof Breakpoint) {
            logger.info("Breakpoint: " + data);
        } else if (data instanceof ExecutionHistory) {
            
        } else if (data instanceof ExecutionSnapshot) {
            
        } else {
            logger.info(data.toString());
        }
    }

    private void displayList(List<?> list) {
        if (list.isEmpty()) {
            logger.info("(empty)");
        } else {
            for (Object item : list) {
                logger.info("  " + item);
            }
        }
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }
}
