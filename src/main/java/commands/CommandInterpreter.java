package commands;

import java.util.*;

public class CommandInterpreter {

    private final Map<String, CommandFactory> commandFactories;
    private final Map<String, String> commandDescriptions;
    private final Map<String, CommandCategory> commandCategories;

    public enum CommandCategory {
        NAVIGATION("Navigation commands"),
        HISTORY("History navigation (replay mode)"),
        INSPECTION("Code inspection"),
        VARIABLES("Variable inspection"),
        BREAKPOINTS("Breakpoint management");

        private final String description;

        CommandCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public CommandInterpreter() {
        this.commandFactories = new HashMap<>();
        this.commandDescriptions = new HashMap<>();
        this.commandCategories = new HashMap<>();
        registerBuiltinCommands();

        final CommandInterpreter self = this;
        registerCommand("help", args -> {
            if (args.length > 0) {
                return new HelpCommand(self, args[0]);
            }
            return new HelpCommand(self);
        }, "Show help: help [command]", CommandCategory.NAVIGATION);
    }

    public void registerCommand(String name, CommandFactory factory,
            String description, CommandCategory category) {
        commandFactories.put(name, factory);
        commandDescriptions.put(name, description);
        commandCategories.put(name, category);
    }

    public void registerSimpleCommand(String name, Command command,
            String description, CommandCategory category) {
        registerCommand(name, args -> command, description, category);
    }

    private void registerBuiltinCommands() {
        
        registerCommand("step", args -> new StepCommand(),
            "Step into next instruction", CommandCategory.NAVIGATION);
        registerCommand("step-over", args -> new StepOverCommand(),
            "Step over current instruction", CommandCategory.NAVIGATION);
        registerCommand("continue", args -> new ContinueCommand(),
            "Continue execution", CommandCategory.NAVIGATION);

        registerCommand("back", args -> new BackCommand(),
            "Go back one step in history", CommandCategory.HISTORY);
        registerCommand("forward", args -> new ForwardCommand(),
            "Go forward one step in history", CommandCategory.HISTORY);
        registerCommand("history", args -> new HistoryCommand(),
            "Show execution history", CommandCategory.HISTORY);

        registerCommand("frame", args -> new FrameCommand(),
            "Show current frame", CommandCategory.INSPECTION);
        registerCommand("temporaries", args -> new TemporariesCommand(),
            "Show temporary variables", CommandCategory.INSPECTION);
        registerCommand("stack", args -> new StackCommand(),
            "Show call stack", CommandCategory.INSPECTION);
        registerCommand("receiver", args -> new ReceiverCommand(),
            "Show receiver (this)", CommandCategory.INSPECTION);
        registerCommand("sender", args -> new SenderCommand(),
            "Show sender frame", CommandCategory.INSPECTION);
        registerCommand("receiver-variables", args -> new ReceiverVariablesCommand(),
            "Show receiver's instance variables", CommandCategory.INSPECTION);
        registerCommand("method", args -> new MethodCommand(),
            "Show current method", CommandCategory.INSPECTION);
        registerCommand("arguments", args -> new ArgumentsCommand(),
            "Show method arguments", CommandCategory.INSPECTION);

        registerCommand("print-var", args -> {
            if (args.length < 1) {
                throw new IllegalArgumentException("print-var requires variable name");
            }
            return new PrintVarCommand(args[0]);
        }, "Print variable value: print-var <name>", CommandCategory.VARIABLES);

        registerCommand("break", args -> {
            if (args.length < 2) {
                throw new IllegalArgumentException("break requires fileName and lineNumber");
            }
            return new BreakCommand(args[0], Integer.parseInt(args[1]));
        }, "Set breakpoint: break <file> <line>", CommandCategory.BREAKPOINTS);

        registerCommand("break-once", args -> {
            if (args.length < 2) {
                throw new IllegalArgumentException("break-once requires fileName and lineNumber");
            }
            return new BreakOnceCommand(args[0], Integer.parseInt(args[1]));
        }, "Set one-time breakpoint: break-once <file> <line>", CommandCategory.BREAKPOINTS);

        registerCommand("break-on-count", args -> {
            if (args.length < 3) {
                throw new IllegalArgumentException("break-on-count requires fileName, lineNumber, and count");
            }
            return new BreakOnCountCommand(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        }, "Set count breakpoint: break-on-count <file> <line> <count>", CommandCategory.BREAKPOINTS);

        registerCommand("breakpoints", args -> new BreakpointsCommand(),
            "List all breakpoints", CommandCategory.BREAKPOINTS);

        registerCommand("break-before-method-call", args -> {
            if (args.length < 1) {
                throw new IllegalArgumentException("break-before-method-call requires method name");
            }
            return new BreakBeforeMethodCallCommand(args[0]);
        }, "Break before method call: break-before-method-call <method>", CommandCategory.BREAKPOINTS);
    }

    public Command parse(String input) throws Exception {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }

        String commandName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        CommandFactory factory = commandFactories.get(commandName);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown command: " + commandName);
        }

        return factory.create(args);
    }

    public Set<String> getAvailableCommands() {
        return commandFactories.keySet();
    }

    public Map<CommandCategory, List<String>> getCommandsByCategory() {
        Map<CommandCategory, List<String>> result = new EnumMap<>(CommandCategory.class);

        for (CommandCategory cat : CommandCategory.values()) {
            result.put(cat, new ArrayList<>());
        }

        for (Map.Entry<String, CommandCategory> entry : commandCategories.entrySet()) {
            result.get(entry.getValue()).add(entry.getKey());
        }

        return result;
    }

    public String getHelp(String commandName) {
        return commandDescriptions.getOrDefault(commandName, "No description available");
    }

    public String getFullHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available commands:\n\n");

        for (CommandCategory cat : CommandCategory.values()) {
            List<String> commands = getCommandsByCategory().get(cat);
            if (!commands.isEmpty()) {
                sb.append("=== ").append(cat.getDescription()).append(" ===\n");
                for (String cmd : commands) {
                    sb.append("  ").append(cmd).append(" - ")
                      .append(commandDescriptions.get(cmd)).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
