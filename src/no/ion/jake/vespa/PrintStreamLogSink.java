package no.ion.jake.vespa;

import no.ion.jake.LogSink;

import java.io.PrintStream;
import java.time.Instant;
import java.util.logging.Level;

class PrintStreamLogSink implements LogSink {
    private final PrintStream out;
    private final Level minLevel;
    private final boolean includeTime;

    PrintStreamLogSink(PrintStream out, Level minLevel, boolean includeTime) {
        this.out = out;
        this.minLevel = minLevel;
        this.includeTime = includeTime;
    }

    @Override
    public boolean isEnabled(Level level) {
        return level.intValue() >= minLevel.intValue();
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        if (!isEnabled(level) || (message == null && throwable == null)) return;

        final String line;
        if (message == null) {
            line = null;
        } else {
            StringBuilder lineBuilder = new StringBuilder(2 + message.length());

            if (includeTime) {
                lineBuilder.append(Instant.now())
                        .append(' ');
            }

            if (level.intValue() >= Level.SEVERE.intValue()) {
                lineBuilder.append("E "); // error
            } else if (level.intValue() >= Level.WARNING.intValue()) {
                lineBuilder.append("W ");
            } else if (level.intValue() >= Level.INFO.intValue()) {
                lineBuilder.append("  ");
            } else if (level.intValue() == Level.FINER.intValue()) {
                // FINER is reserved for stdout/stderr
                lineBuilder.append("O ");
            } else { // Below INFO but not FINER.  Includes FINE and FINEST. Classified as "debug".
                lineBuilder.append("D ");
            }

            lineBuilder.append(message);
            line = lineBuilder.toString();
        }

        // PrintStream's println() does a synchronized(this), so we'll reuse that monitor.
        synchronized (out) {
            if (line != null) {
                out.println(line);
            }
            if (throwable != null) {
                throwable.printStackTrace(out);
            }
        }
    }
}
