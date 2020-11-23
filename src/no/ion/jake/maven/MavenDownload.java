package no.ion.jake.maven;

import no.ion.jake.BuildContext;
import no.ion.jake.Project;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class MavenDownload implements Build {
    private final Project project;
    private final MavenCentral mavenCentral;
    private final MavenArtifact artifact;
    private final Path path;

    public MavenDownload(Project project, MavenCentral mavenCentral, MavenArtifact artifact) {
        this.project = project;
        this.mavenCentral = mavenCentral;
        this.artifact = artifact;
        this.path = project.pathToMavenRepository().resolve(artifact.toRepoPath());
    }

    @Override
    public BuildResult build(BuildContext buildContext) {
        if (Files.isRegularFile(path)) {
            return BuildResult.ofNoop();
        }

        mavenCentral.downloadTo(artifact, path);

        return BuildResult.of("downloaded " + artifact.toCoordinate());
    }
}
