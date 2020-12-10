package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.api.Artifact2;
import no.ion.jake.maven.MavenArtifactId;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class Artifact2Impl implements Artifact2 {
    private final MavenArtifactId mavenArtifactId;
    private final Path localMavenRepo;

    public Artifact2Impl(MavenArtifactId mavenArtifactId, Path localMavenRepo) {
        this.mavenArtifactId = mavenArtifactId;
        this.localMavenRepo = localMavenRepo;
    }

    @Override
    public String getId() {
        // This is based on Maven's MavenArtifactId.getId(), which returns GROUP:ID:TYPE[:CLASSIFIER]:VERSION. (TYPE ~ packaging).
        var id = new StringBuilder()
                .append(mavenArtifactId.groupId())
                .append(':').append(mavenArtifactId.artifactId())
                .append(':').append(mavenArtifactId.packaging());
        mavenArtifactId.optionalClassifier().ifPresent(classifier -> id.append(':').append(classifier));
        id.append(':').append(mavenArtifactId.version());
        return id.toString();
    }

    @Override public boolean hasClassifier() { return mavenArtifactId.optionalClassifier().isPresent(); }
    @Override public String getType() { return mavenArtifactId.packaging(); }
    @Override public String getScope() { return mavenArtifactId.scope().toMavenScope(); }
    @Override public File getFile() { return localMavenRepo.resolve(mavenArtifactId.toRepoPath()).toFile(); }
    @Override public String getArtifactId() { return mavenArtifactId.artifactId(); }
    @Override public String getGroupId() { return mavenArtifactId.groupId(); }
    @Override public String getClassifier() { return mavenArtifactId.optionalClassifier().orElse(null); }
    @Override public String getVersion() { return mavenArtifactId.version(); }

    @Override
    public List<String> getDependencyTrail() {
        throw new UnsupportedOperationException();
    }
}
