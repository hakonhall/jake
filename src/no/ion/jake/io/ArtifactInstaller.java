package no.ion.jake.io;

import no.ion.jake.BuildContext;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;

public class ArtifactInstaller {
    private final BuildContext context;

    public ArtifactInstaller(BuildContext context) {
        this.context = context;
    }

    public ArtifactInstaller install(String path, MavenArtifact artifactId) {
        return install(context.moduleContext().project().fileSystem().getPath(path), artifactId);
    }

    public ArtifactInstaller install(Path path, MavenArtifact artifactId) {
        String repoPath = artifactId.toRepoPath();
        Path toPath = context.moduleContext().project().pathToMavenRepository().resolve(repoPath);
        Copy.copyFile(path, toPath).install(context);
        context.logInfo("installed " + path + " as " + repoPath);
        return this;
    }
}
