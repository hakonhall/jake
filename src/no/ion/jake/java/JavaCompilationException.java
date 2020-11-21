package no.ion.jake.java;

import no.ion.jake.JakeException;

import static java.util.Objects.requireNonNull;

public class JavaCompilationException extends JakeException {
    public JavaCompilationException(String message) {
        super(requireNonNull(message));
    }
}
