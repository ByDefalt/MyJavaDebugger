package io;

/**
 * Logger pour la console (SRP - Single Responsibility)
 * Responsabilité : afficher les logs dans le terminal
 */
public class ConsoleLogger implements Logger {

    private Level minLevel = Level.INFO;

    public ConsoleLogger() {}

    public ConsoleLogger(Level minLevel) {
        this.minLevel = minLevel;
    }

    public void setMinLevel(Level level) {
        this.minLevel = level;
    }

    @Override
    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    @Override
    public void debug(String format, Object... args) {
        log(Level.DEBUG, String.format(format, args));
    }

    @Override
    public void info(String message) {
        log(Level.INFO, message);
    }

    @Override
    public void info(String format, Object... args) {
        log(Level.INFO, String.format(format, args));
    }

    @Override
    public void warn(String message) {
        log(Level.WARN, message);
    }

    @Override
    public void warn(String format, Object... args) {
        log(Level.WARN, String.format(format, args));
    }

    @Override
    public void error(String message) {
        log(Level.ERROR, message);
    }

    @Override
    public void error(String format, Object... args) {
        log(Level.ERROR, String.format(format, args));
    }

    @Override
    public void error(String message, Throwable t) {
        log(Level.ERROR, message + ": " + t.getMessage());
    }

    @Override
    public void log(Level level, String message) {
        if (level.ordinal() < minLevel.ordinal()) {
            return;
        }

        String formatted = "[" + level.name() + "] " + message;

        if (level == Level.ERROR) {
            System.err.println(formatted);
        } else {
            System.out.println(formatted);
        }
    }
}
