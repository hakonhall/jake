package no.ion.jake.maven;

import no.ion.jake.Project;

import java.nio.file.Path;

public class ArtifactHandle {
    private final MavenArtifact artifact;

    private final Object monitor = new Object();
    private State state = State.INITIAL;

    public ArtifactHandle(Project project, MavenArtifact artifact) {
        this.artifact = artifact;
    }

    public MavenArtifact artifact() { return artifact; }

    private enum State { INITIAL, EXISTS_IN_LOCAL, MISSING_IN_LOCAL, DOWNLOADING, DOWNLOAD_FAILED }
}
