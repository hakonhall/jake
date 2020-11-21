package no.ion.jake.junit4;

public class JUnit4TestResults {
    private final boolean success;
    private final String message;

    public JUnit4TestResults(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean success() { return success; }
    public String message() { return message; }

    @Override
    public String toString() { return message; }
}
