package no.ion.jake.engine;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class BuildResult {
    private final Duration duration;
    private final Set<ArtifactId> artifactIds;
    private final Optional<RuntimeException> exception;
    private final Optional<Error> error;

    public static BuildResult fromSuccess(BuildContextImpl context) {
        return new BuildResult(context.durationUpToNow(), context.publishedArtifacts(), null, null);
    }

    public static BuildResult fromException(BuildContextImpl context, RuntimeException exception) {
        return new BuildResult(context.durationUpToNow(), context.publishedArtifacts(), exception, null);
    }

    public static BuildResult fromError(BuildContextImpl context, Error error) {
        return new BuildResult(context.durationUpToNow(), context.publishedArtifacts(), null, error);
    }

    private BuildResult(Duration duration, Set<ArtifactId> artifactIds, RuntimeException exception, Error error) {
        this.duration = duration;
        this.artifactIds = Objects.requireNonNull(artifactIds, "artifactIds cannot be null");
        this.exception = Optional.ofNullable(exception);
        this.error = Optional.ofNullable(error);
    }

    public boolean success() {
        return exception.isEmpty() && error.isEmpty();
    }

    public Optional<RuntimeException> getRuntimeException() {
        return exception;
    }

    public Optional<Error> getError() {
        return error;
    }

    public Set<ArtifactId> publishedArtifacts() {
        return artifactIds;
    }
}
