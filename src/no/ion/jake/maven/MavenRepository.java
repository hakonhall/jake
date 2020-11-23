package no.ion.jake.maven;

import no.ion.jake.Project;

import java.util.HashMap;

public class MavenRepository {
    private final Project project;
    private final MavenCentral mavenCentral;

    private final Object monitor = new Object();
    private final HashMap<MavenArtifact, ArtifactHandle> artifacts = new HashMap<>();

    public MavenRepository(Project project, MavenCentral mavenCentral) {
        this.project = project;
        this.mavenCentral = mavenCentral;
    }

    public MavenDownload scheduleDownloadOf(MavenArtifact artifact) {
        return new MavenDownload(project, mavenCentral, artifact);
    }

    private ArtifactHandle getArtifactInfoLocked(MavenArtifact artifact) {
        return artifacts.computeIfAbsent(artifact, __ -> new ArtifactHandle(project, artifact));
    }
}
