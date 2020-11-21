package no.ion.jake.java;

import java.time.Duration;
import java.util.Optional;

public class JavaArchiverResult {
    private final String warning;
    private final Duration duration;

    public JavaArchiverResult(String warning, Duration duration) {
        this.warning = warning;
        this.duration = duration;
    }

    public Optional<String> warning() {
        return warning.isEmpty() ? Optional.empty() : Optional.of(warning);
    }

    public Duration duration() { return duration; }

    /** Returns the duration of the archiving command, in seconds, rounded down to millisecond precision. */
    public double getSeconds() { return duration.toMillis() / 1000.0; }
}
