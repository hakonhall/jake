package no.ion.jake;

import no.ion.jake.build.Build;
import no.ion.jake.build.BuildResult;
import no.ion.jake.module.ModuleContext;
import no.ion.jake.util.Stopwatch;

import java.time.Duration;
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

    public void run(Build build) {
        var runningStopwatch = Stopwatch.start();
        BuildResult result = build.build(this);

        if (result.noop()) {
            String summary = result.summary();
            logDebug(summary.isEmpty() ? "noop" : summary);
        } else if (!result.summary().isEmpty()) {
            if (result.appendDuration()) {
                Duration duration = runningStopwatch.stop();
                String time = String.format(" in %.3f s", duration.toMillis() / 1000.0);
                logInfo(result.summary() + time);
            } else {
                logInfo(result.summary());
            }
        }
    }

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
