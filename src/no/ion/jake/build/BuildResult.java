package no.ion.jake.build;

import no.ion.jake.BuildContext;

import java.util.Objects;

public interface BuildResult {
    /** Whether the build was a waste of time and its outputs were effectively unchanged. */
    default boolean noop() { return false; }

    /** Summary to print, e.g. "compiled 23 sources to target/classes". If null or empty, nothing will be printed. */
    String summary();

    /**
     * Whether the client may measure the duration of the {@link Build#build(BuildContext) build()} and
     * append a string of the format " in 12.345 s" to the summary, when or if printing the summary.
     */
    default boolean appendDuration() { return true; }

    static BuildResult of(String summary) { return of(false, summary, true); }
    static BuildResult ofSilentSuccess() { return of(""); }
    static BuildResult ofNoop() { return of(true, "", false); }
    static BuildResult of(boolean noop, String summary, boolean appendDuration) {
        return new BuildResult() {
            @Override public boolean noop() { return noop; }
            @Override public String summary() { return summary == null ? "" : summary; }
            @Override public boolean appendDuration() { return appendDuration; }
        };
    }
}
