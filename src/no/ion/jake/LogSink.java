package no.ion.jake;

import java.util.logging.Level;

public interface LogSink {
    boolean isEnabled(Level level);
    /** Level.FINER is used for redirected stdout/stderr. */
    void log(Level level, String message, Throwable throwable);
}
