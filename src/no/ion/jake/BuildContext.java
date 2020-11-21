package no.ion.jake;

import java.util.function.Supplier;
import java.util.logging.Level;

public class BuildContext {
    private final ModuleContext module;
    private final LogSink logSink;

    public BuildContext(ModuleContext module, LogSink logSink) {
        this.module = module;
        this.logSink = logSink;
    }

    public ModuleContext moduleContext() { return module; }

    public void logSevereProblemAndExitJVM(String message) {
        logSink.log(Level.SEVERE, module.name() + " " + message, new RuntimeException(message));
        System.exit(1);
    }

    public void logError(String message) {
        logSink.log(Level.SEVERE, message, null);
    }

    public void logWarning(String message) {
        logSink.log(Level.WARNING, module.name() + " " + message, null);
    }

    public void logWarning(String message, Throwable cause) {
        logSink.log(Level.WARNING, (message.isEmpty() ? "" : message + ": ") + cause.toString(), null);
    }

    public void logInfo(String message, Object... formatArguments) {
        if (formatArguments.length > 0) {
            logInfo(String.format(message, formatArguments));
        } else {
            logSink.log(Level.INFO, module.name() + " " + message, null);
        }
    }

    public void logInfoFormat(String format, Object... arguments) {
        logInfo(String.format(format, arguments));
    }

    public boolean isDebugEnabled() { return logSink.isEnabled(Level.FINE); }

    public void logDebug(String message) {
        logSink.log(Level.FINE, module.name() + ' ' + message, null);
    }

    public void logDebug(Supplier<String> message) {
        if (logSink.isEnabled(Level.FINE)) {
            logSink.log(Level.FINE, module.name() + " " + message.get(), null);
        }
    }
}
