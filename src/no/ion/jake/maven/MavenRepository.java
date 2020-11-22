package no.ion.jake.maven;

public interface MavenRepository {
    /**
     * The client requires the given artifact, and gets back a handle that can be used to locate the artifact
     * once it is available.
     */
    MavenArtifactLocationHandle download(MavenArtifact artifact);
}
