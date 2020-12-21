package no.ion.jake.maven;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;

import java.nio.file.Files;
import java.nio.file.Path;

public class MavenDownload implements Build {
    private final MavenCentral mavenCentral;
    private final Path localRepo;
    private final MavenArtifact mavenArtifact;

    public MavenDownload(MavenCentral mavenCentral, Path localRepo, MavenArtifactId id, Artifact<Path> jarPathArtifact) {
        this.mavenCentral = mavenCentral;
        this.localRepo = localRepo;
        this.mavenArtifact = new MavenArtifact(id, jarPathArtifact);
    }

    @Override
    public String name() {
        return "downloading " + mavenArtifact.id().toCoordinate();
    }

    public MavenArtifact mavenArtifact() {
        return mavenArtifact;
    }

    @Override
    public void build(BuildContext buildContext) {
        Path path = localRepo.resolve(mavenArtifact.id().toRepoPath());

        if (Files.isRegularFile(path)) {
            buildContext.newPublicationOf(mavenArtifact.pathArtifact())
                    .hasChanged(false)
                    .publish(path);
            return;
        }

        mavenCentral.downloadTo(mavenArtifact.id(), path);

        buildContext.newPublicationOf(mavenArtifact.pathArtifact())
                .logWithDuration("downloaded " + mavenArtifact.id().toCoordinate())
                .publish(path);
    }
}
