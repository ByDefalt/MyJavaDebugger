package commands;


@FunctionalInterface
interface CommandFactory {
    Command create(String[] args) throws Exception;
}
