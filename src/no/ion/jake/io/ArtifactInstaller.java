package no.ion.jake.io;

import no.ion.jake.BuildContext;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;

public class ArtifactInstaller {
    private final BuildContext context;

    public ArtifactInstaller(BuildContext context) {
        this.context = context;
    }

    public ArtifactInstall of(String path, MavenArtifact artifactId) {
        return of(context.moduleContext().project().fileSystem().getPath(path), artifactId);
    }

    public ArtifactInstall of(Path path, MavenArtifact artifactId) {
        return new ArtifactInstall(path, artifactId);
    }
}
