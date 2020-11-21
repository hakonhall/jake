package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.api.Artifact2;
import no.ion.jake.maven.MavenArtifact;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class Artifact2Impl implements Artifact2 {
    private final MavenArtifact mavenArtifact;
    private final Path localMavenRepo;

    public static Artifact2Impl fromMavenArtifact(MavenArtifact mavenArtifact, Path localMavenRepo) {
        return new Artifact2Impl(mavenArtifact, localMavenRepo);
    }

    public Artifact2Impl(MavenArtifact mavenArtifact, Path localMavenRepo) {
        this.mavenArtifact = mavenArtifact;
        this.localMavenRepo = localMavenRepo;
    }

    @Override
    public String getId() {
        // This is based on Maven's MavenArtifact.getId(), which returns GROUP:ID:TYPE[:CLASSIFIER]:VERSION. (TYPE ~ packaging).
        var id = new StringBuilder()
                .append(mavenArtifact.groupId())
                .append(':').append(mavenArtifact.artifactId())
                .append(':').append(mavenArtifact.packaging());
        mavenArtifact.optionalClassifier().ifPresent(classifier -> id.append(':').append(classifier));
        id.append(':').append(mavenArtifact.version());
        return id.toString();
    }

    @Override public boolean hasClassifier() { return mavenArtifact.optionalClassifier().isPresent(); }
    @Override public String getType() { return mavenArtifact.packaging(); }
    @Override public String getScope() { return mavenArtifact.scope().toMavenScope(); }
    @Override public File getFile() { return localMavenRepo.resolve(mavenArtifact.toRepoPath()).toFile(); }
    @Override public String getArtifactId() { return mavenArtifact.artifactId(); }
    @Override public String getGroupId() { return mavenArtifact.groupId(); }
    @Override public String getClassifier() { return mavenArtifact.optionalClassifier().orElse(null); }
    @Override public String getVersion() { return mavenArtifact.version(); }

    @Override
    public List<String> getDependencyTrail() {
        throw new UnsupportedOperationException();
    }
}
