package io;

import java.util.Scanner;

public class ConsoleInputReader implements InputReader {

    private final Scanner scanner;

    public ConsoleInputReader() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public String readLine() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        }
        return null;
    }

    @Override
    public String readLine(String prompt) {
        System.out.print(prompt);
        return readLine();
    }

    @Override
    public boolean hasInput() {
        return scanner.hasNextLine();
    }

    @Override
    public void close() {
        scanner.close();
    }
}
