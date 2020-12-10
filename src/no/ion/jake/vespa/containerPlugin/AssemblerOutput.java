package no.ion.jake.vespa.containerPlugin;

import no.ion.jake.build.Artifact;

import java.nio.file.Path;

public class AssemblerOutput {
    private final Artifact<Path> jarArtifact;
    private final Artifact<Path> jarWithDependenciesArtifact;

    public AssemblerOutput(Artifact<Path> jarArtifact, Artifact<Path> jarWithDependenciesArtifact) {
        this.jarArtifact = jarArtifact;
        this.jarWithDependenciesArtifact = jarWithDependenciesArtifact;
    }

    public Artifact<Path> jarArtifact() { return jarArtifact; }
    public Artifact<Path> jarWithDependenciesArtifact() { return jarWithDependenciesArtifact; }
}
