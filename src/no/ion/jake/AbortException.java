package no.ion.jake;

public class AbortException extends JakeException {
    public AbortException() {}
    /** Print error message before aborting build. */
    public AbortException(String message) { super(message); }
}
