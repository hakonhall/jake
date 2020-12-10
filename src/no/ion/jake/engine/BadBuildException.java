package no.ion.jake.engine;

import no.ion.jake.JakeException;
import no.ion.jake.build.Build;

public class BadBuildException extends JakeException {
    public BadBuildException(Build build, String message) {
        super("bad build " + build.name() + ": " + message);
    }

    public BadBuildException(Build build, Exception e) {
        super("bad build " + build.name(), e);
    }
}
