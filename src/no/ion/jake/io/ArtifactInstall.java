package no.ion.jake.io;

import no.ion.jake.BuildContext;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildResult;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;

public class ArtifactInstall implements Build {
    private final Path path;
    private final MavenArtifact artifactId;

    public ArtifactInstall(Path path, MavenArtifact artifactId) {
        this.path = path;
        this.artifactId = artifactId;
    }

    @Override
    public BuildResult build(BuildContext buildContext) {
        String repoPath = artifactId.toRepoPath();
        Path toPath = buildContext.moduleContext().project().pathToMavenRepository().resolve(repoPath);
        Copy.copyFile(path, toPath).install(buildContext);
        return BuildResult.of(false, "installed " + path + " as " + repoPath, false);
    }
}
