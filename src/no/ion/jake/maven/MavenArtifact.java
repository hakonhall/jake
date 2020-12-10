package no.ion.jake.maven;

import no.ion.jake.build.Artifact;

import java.nio.file.Path;

public class MavenArtifact {
    private final MavenArtifactId id;
    private final Artifact<Path> pathArtifact;

    public MavenArtifact(MavenArtifactId id, Artifact<Path> pathArtifact) {
        this.id = id;
        this.pathArtifact = pathArtifact;
    }

    public MavenArtifactId id() { return id; }
    public Artifact<Path> pathArtifact() { return pathArtifact; }
}
