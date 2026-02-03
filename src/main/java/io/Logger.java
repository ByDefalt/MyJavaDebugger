package io;

public interface Logger {

    enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    void debug(String message);
    void debug(String format, Object... args);

    void info(String message);
    void info(String format, Object... args);

    void warn(String message);
    void warn(String format, Object... args);

    void error(String message);
    void error(String format, Object... args);
    void error(String message, Throwable t);

    void log(Level level, String message);
}
