package no.ion.jake.build;

public interface ArtifactPublisher<T> {
    void publish(T artifact);

    Artifact<T> getArtifact();
}
