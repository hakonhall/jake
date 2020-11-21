package no.ion.jake.util;

import java.time.Duration;

/**
 * Stopwatch is optimized to measure elapsed time ONLY.
 *
 * <p>Typically, you're more interested in both a start timestamp (that can be converted to e.g. an Instant)
 * and elapsed time, and in case you need something else (ideally based on {@link System#currentTimeMillis()}).</p>
 */
public class Stopwatch {
    public static Running start() {
        return new Running(System.nanoTime());
    }

    public static class Running {
        private final long startNanos;

        private Running(long startNanos) {
            this.startNanos = startNanos;
        }

        /** Returns the duration from start to now.  May be called multiple times. Thread-safe. */
        public Duration stop() {
            return Duration.ofNanos(System.nanoTime() - startNanos);
        }
    }
}
