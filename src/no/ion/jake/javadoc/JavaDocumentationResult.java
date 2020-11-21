package no.ion.jake.javadoc;

import java.time.Duration;

public class JavaDocumentationResult {
    private final String out;
    private final Duration duration;

    public JavaDocumentationResult(String out, Duration duration) {
        this.out = out;
        this.duration = duration;
    }

    public String warning() { return out; }
    public Duration duration() { return duration; }
    public double getSeconds() { return duration.toMillis() / 1000.0; }
}
