package io;
public interface InputReader {
    String readLine();
    String readLine(String prompt);
    boolean hasInput();
    void close();
}
