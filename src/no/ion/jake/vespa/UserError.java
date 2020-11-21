package no.ion.jake.vespa;

public class UserError extends RuntimeException {
    public UserError(String message) {
        super(message);
    }
}
