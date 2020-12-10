package no.ion.jake.engine;

import no.ion.jake.build.Artifact;

@FunctionalInterface
public interface ArtifactRegistry {
    <T> ArtifactImpl<T> verifyArtifact(Artifact<T> artifact, BuildId buildId);
}
