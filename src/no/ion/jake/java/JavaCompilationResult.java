package no.ion.jake.java;

import java.time.Duration;
import java.util.Optional;

public class JavaCompilationResult {
    private final int numFilesCompiled;
    private final Optional<String> warning;
    private final Duration duration;

    public JavaCompilationResult(int numFilesCompiled, String warning, Duration duration) {
        this.numFilesCompiled = numFilesCompiled;
        this.warning = warning == null || warning.isBlank() ? Optional.empty() : Optional.of(warning);
        this.duration = duration;

        if (numFilesCompiled < 0) {
            throw new IllegalArgumentException("numFilesCompiled is negative: " + numFilesCompiled);
        }
    }

    public int numFilesCompiled() { return numFilesCompiled; }
    public Optional<String> warning() { return warning; }
    public Duration getDuration() { return duration; }
    /** Get the time of compilation in seconds, truncated down to a whole millisecond. */
    public double getSeconds() { return getMillis() / 1000.0; }
    public long getMillis() { return duration.toMillis(); }

    @Override
    public String toString() {
        return "JavaCompilationResult{" +
                ", numFilesCompiled=" + numFilesCompiled +
                ", warning=" + warning +
                '}';
    }
}
