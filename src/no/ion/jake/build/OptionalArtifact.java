package no.ion.jake.build;

import java.util.Optional;

/** An interface for something that MAY be backed by an artifact, and a way to get the instance during build. */
public interface OptionalArtifact<T> {
    Optional<Artifact<?>> getArtifact();
    T get();

    static <A> OptionalArtifact<A> ofArtifact(Artifact<A> artifact) {
        return new OptionalArtifact<A>() {
            @Override public Optional<Artifact<?>> getArtifact() { return Optional.of(artifact); }
            @Override public A get() { return artifact.detail(); }
        };
    }

    static <A> OptionalArtifact<A> ofInstance(A instance) {
        return new OptionalArtifact<A>() {
            @Override public Optional<Artifact<?>> getArtifact() { return Optional.empty(); }
            @Override public A get() { return instance; }
        };
    }
}
