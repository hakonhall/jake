package no.ion.jake.maven;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MavenRepository {
    private final Object monitor = new Object();
    private final Map<MavenArtifactId, MavenDownload> downloads = new HashMap<>();

    private final Path localRepo;
    private final MavenCentral mavenCentral;

    public MavenRepository(Path localRepo, MavenCentral mavenCentral) {
        this.localRepo = localRepo;
        this.mavenCentral = mavenCentral;
    }

    public Path localRepositoryPath() { return localRepo; }

    public MavenArtifact declareDownload(Declarator declarator, MavenArtifactId mavenArtifactId) {
        synchronized (monitor) {
            MavenDownload download = downloads.get(mavenArtifactId);
            if (download != null) {
                return download.mavenArtifact();
            }

            try (var declaration = declarator.declareNewBuild("maven")) {
                Artifact<Path> artifact = declaration.producesArtifact(Path.class, mavenArtifactId.toCoordinate());
                MavenDownload mavenDownload = new MavenDownload(mavenCentral, localRepo, mavenArtifactId, artifact);
                declaration.forBuild(mavenDownload);
                downloads.put(mavenArtifactId, mavenDownload);
                return mavenDownload.mavenArtifact();
            }
        }

    }
}
