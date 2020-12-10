package no.ion.jake.build;

import no.ion.jake.LogSink;

import java.util.function.Supplier;
import java.util.logging.Level;

public class Logger {
    private final LogSink logSink;
    private final String contextName;

    public Logger(LogSink logSink, String contextName) {
        this.logSink = logSink;
        this.contextName = contextName;
    }

    public void error(String message) {
        logSink.log(Level.SEVERE, message, null);
    }

    public void warning(String message) {
        logSink.log(Level.WARNING, contextName + " " + message, null);
    }

    public void warning(String message, Throwable cause) {
        logSink.log(Level.WARNING, (message.isEmpty() ? "" : message + ": ") + cause.toString(), null);
    }

    public void info(String message, Object... formatArguments) {
        if (formatArguments.length > 0) {
            info(String.format(message, formatArguments));
        } else {
            logSink.log(Level.INFO, contextName + " " + message, null);
        }
    }

    public void infoFormat(String format, Object... arguments) {
        info(String.format(format, arguments));
    }

    public boolean isDebugEnabled() { return logSink.isEnabled(Level.FINE); }

    public void debug(String message) {
        logSink.log(Level.FINE, contextName + ' ' + message, null);
    }

    public void debug(Supplier<String> message) {
        if (logSink.isEnabled(Level.FINE)) {
            logSink.log(Level.FINE, contextName + " " + message.get(), null);
        }
    }
}
