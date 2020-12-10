package no.ion.jake;

public class JakeException extends RuntimeException {
    public JakeException() {}
    public JakeException(String message) { super(message); }
    public JakeException(Exception e) { super(e); }
    public JakeException(String message, Exception e) { super(message, e); }
}
