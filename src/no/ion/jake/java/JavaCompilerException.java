package no.ion.jake.java;

import no.ion.jake.JakeException;

import static java.util.Objects.requireNonNull;

public class JavaCompilerException extends JakeException {
    public JavaCompilerException(String message) {
        super(requireNonNull(message));
    }
}
