package no.ion.jake.engine;

import java.util.Objects;
import java.util.Optional;

public class ArtifactId {
    private final Optional<String> moduleName;
    private final String artifactName;

    public ArtifactId(String moduleNameOrNull, String artifactName) {
        this.moduleName = Optional.ofNullable(moduleNameOrNull);
        this.artifactName = Objects.requireNonNull(artifactName, "artifactName must be non-null");
    }

    public Optional<String> moduleName() { return moduleName; }
    public String artifactName() { return artifactName; }

    @Override
    public String toString() {
        return moduleName().map(name -> artifactName + " in module " + name).orElseGet(() -> "global artifact " + artifactName);
    }
}
