package no.ion.jake;

/**
 * Indicates the end-user has done something wrong, e.g. we have been unable to locate a dependency with a user
 * provided coordinate.  The message should therefore be user friendly, and no stack trace will be printed.
 * All messages will be given a common prefix, e.g. "error: " or "" (no prefix).
 */
public class UserError extends JakeException {
    public UserError(String message) {
        super("error: " + message);
    }
}
