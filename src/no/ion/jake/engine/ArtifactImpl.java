package no.ion.jake.engine;

import no.ion.jake.build.Artifact;

import java.util.Objects;
import java.util.Optional;

public class ArtifactImpl<T> implements Artifact<T> {
    private final ArtifactId artifactId;
    private final Class<T> artifactClass;

    // Set when (if) the build is defined
    private BuildId buildId = null;

    private final Object monitor = new Object();
    private T instance = null;

    public ArtifactImpl(ArtifactId artifactId, Class<T> artifactClass) {
        this.artifactId = artifactId;
        this.artifactClass = artifactClass;
    }

    public ArtifactId artifactId() { return artifactId; }
    @Override public Optional<String> moduleName() { return artifactId.moduleName(); }
    @Override public String name() { return artifactId.artifactName(); }
    public Class<T> instanceClass() { return artifactClass; }
    public BuildId buildId() { return Objects.requireNonNull(buildId, "build ID has not yet been set"); }

    public void publish(T instance) {
        if (artifactClass == Void.class) {
            if (instance != null) {
                throw new IllegalArgumentException("non-null Void instance");
            }
            return;
        }

        Objects.requireNonNull(instance, "published instance cannot be null");
        if (!artifactClass.isInstance(instance)) {
            throw new ClassCastException("published intance has incompatible type " + instance.getClass().getName() +
                    ": expected " + artifactClass.getName());
        }

        synchronized (monitor) {
            this.instance = instance;
        }
    }

    @Override
    public T detail() {
        synchronized (monitor) {
            if (instance == null) {
                throw new IllegalStateException("artifact " + toString() + " has not been published");
            }
            return instance;
        }
    }

    @Override
    public String toString() {
        return artifactId.toString() + " of type " + artifactClass.getName();
    }

    public void setBuildId(BuildId buildId) {
        this.buildId = Objects.requireNonNull(buildId);
    }
}
