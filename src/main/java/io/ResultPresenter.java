package io;
import commands.CommandResult;
public interface ResultPresenter {
    void displayResult(CommandResult result);
    void info(String message);
    void error(String message);
    void warn(String message);
}
