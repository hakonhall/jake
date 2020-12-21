package no.ion.jake.engine;

import java.util.Objects;
import java.util.Optional;

public class ArtifactId {
    private final String namespace;
    private final String artifactName;

    public ArtifactId(String namespace, String artifactName) {
        this.namespace = Objects.requireNonNull(namespace, "namespace must be non-null");
        this.artifactName = Objects.requireNonNull(artifactName, "artifactName must be non-null");
    }

    public String namespace() { return namespace; }
    public String artifactName() { return artifactName; }

    @Override
    public String toString() {
        return "ArtifactId{" + namespace + ":" + artifactName + "}";
    }
}
