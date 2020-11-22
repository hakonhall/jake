package no.ion.jake.build;

import no.ion.jake.BuildContext;

public interface BuildResult {
    /** Whether the build was a waste of time and its outputs were effectively unchanged. */
    default boolean noop() { return false; }

    /** Summary to print, e.g. "compiled 23 sources to target/classes". */
    String summary();

    /**
     * Whether the client may measure the duration of the {@link Build#build(BuildContext) build()} and
     * append a string of the format " in 12.345 s" to the summary, when or if printing the summary.  If false,
     * the {@code build} is expected to embed timing information in the summary itself.
     */
    default boolean appendDuration() { return true; }

    static BuildResult of(String summary) { return of(false, summary, true); }
    static BuildResult of(boolean noop, String summary, boolean appendDuration) {
        return new BuildResult() {
            @Override public boolean noop() { return noop; }
            @Override public String summary() { return summary; }
            @Override public boolean appendDuration() { return appendDuration; }
        };
    }
}
