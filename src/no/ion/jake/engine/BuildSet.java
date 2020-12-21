package no.ion.jake.engine;

import no.ion.jake.LogSink;
import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.graph.BuildMeta;
import no.ion.jake.graph.BuildOrder;
import no.ion.jake.graph.BuildOrderImpl;
import no.ion.jake.graphviz.Graphviz;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class BuildSet {
    private final Object monitor = new Object();
    private final HashMap<ArtifactId, ArtifactImpl<?>> artifacts = new HashMap<>();
    private final HashMap<BuildId, BuildInfo> builds = new HashMap<>();
    private final JakeExecutor jakeExecutor;
    private final LogSink logSink;

    public BuildSet(JakeExecutor jakeExecutor, LogSink logSink) {
        this.jakeExecutor = jakeExecutor;
        this.logSink = logSink;
    }

    public <T> ArtifactImpl<T> newArtifact(Class<T> artifactClass, String namespace, String name) {
        var artifactId = new ArtifactId(namespace, name);
        var artifact = new ArtifactImpl<>(artifactId, artifactClass);

        synchronized (monitor) {
            if (artifacts.put(artifactId, artifact) != null) {
                throw new IllegalArgumentException("duplicate artifact: " + artifactId.toString());
            }
        }

        return artifact;
    }

    public void addBuild(ModuleContext moduleContext, String namespace, Build build, List<Artifact<?>> dependencies,
                         Set<ArtifactImpl<?>> production) {
        Set<BuildId> buildIdDependencies = new HashSet<>();

        Set<ArtifactId> dependencySet = dependencies.stream()
                .map(this::verifyArtifact)
                .peek(artifactImpl -> buildIdDependencies.add(artifactImpl.buildId()))
                .map(ArtifactImpl::artifactId)
                .collect(Collectors.toSet());

        BuildId buildId = new BuildId(namespace, build.name());
        Set<ArtifactId> productionSet = production.stream()
                // TODO: Re-evaluate thread-safety
                .peek(artifactImpl -> artifactImpl.setBuildId(buildId))
                .map(ArtifactImpl::artifactId)
                .collect(Collectors.toSet());

        var info = new BuildInfo(buildId, moduleContext, namespace, build, dependencySet, productionSet, buildIdDependencies);
        synchronized (monitor) {
            if (!artifacts.keySet().containsAll(dependencySet)) {
                throw new IllegalArgumentException(buildId.toString() + " depends on artifacts that no-one are producing");
            }

            if (builds.putIfAbsent(buildId, info) != null) {
                throw new IllegalArgumentException("duplicate build: " + buildId.toString());
            }
        }
    }

    public void buildEverything() {
        List<BuildMeta<BuildId>> buildMetas = builds.values().stream()
                .map(buildInfo -> new BuildMeta<BuildId>() {
                    @Override
                    public BuildId id() {
                        return buildInfo.id();
                    }

                    @Override
                    public Duration expectedBuildDuration() {
                        // TODO: Get this from earlier runs
                        return Duration.ofMillis(100);
                    }

                    @Override
                    public float expectedLoad() {
                        // TODO: Get this from earlier runs.
                        return 1f;
                    }

                    @Override
                    public Set<BuildId> dependencies() {
                        return buildInfo.buildDependencies();
                    }

                    @Override
                    public String toString() {
                        return "BuildMeta{" + buildInfo.id() + "}";
                    }
                })
                .collect(Collectors.toList());

        BuildOrder<BuildId> buildOrder = BuildOrderImpl.make(buildMetas);
        try (BuildGraph buildGraph = new BuildGraph(jakeExecutor, jakeExecutor.targetLoad(), buildOrder, builds.values(),
                this::verifyArtifact, logSink)) {
            buildGraph.runSync();
        }

    }

    public ArtifactImpl<?> verifyArtifact(Artifact<?> artifact) {
        Objects.requireNonNull(artifact, "artifact cannot be null");

        if (!(artifact instanceof ArtifactImpl)) {
            throw new IllegalArgumentException("not an artifact of this build graph: " + artifact.toString());
        }
        ArtifactImpl<?> artifactImpl = (ArtifactImpl<?>) artifact;

        ArtifactId artifactId = artifactImpl.artifactId();
        ArtifactImpl<?> ourArtifactImpl;
        synchronized (monitor) {
            ourArtifactImpl = artifacts.get(artifactId);
        }
        Objects.requireNonNull(ourArtifactImpl, "no such artifact: " + artifact.toString());

        if (ourArtifactImpl != artifact) {
            // Not sure how this can happen...
            throw new IllegalStateException("two artifacts with the same id: " + artifact.toString());
        }

        return artifactImpl;
    }

    /** Verify this graph owns the artifact, and that it belongs to the given build. */
    public <T> ArtifactImpl<T> verifyArtifact(Artifact<T> artifact, BuildId buildId) {
        Objects.requireNonNull(artifact, "artifact cannot be null");
        Objects.requireNonNull(buildId, "buildId cannot be null");

        if (!(artifact instanceof ArtifactImpl)) {
            throw new IllegalArgumentException("not an artifact of this build graph: " + artifact.toString());
        }
        ArtifactImpl<T> artifactImpl = (ArtifactImpl<T>) artifact;

        ArtifactId artifactId = artifactImpl.artifactId();
        ArtifactImpl<?> ourArtifactImpl;
        BuildInfo buildInfo;
        synchronized (monitor) {
            ourArtifactImpl = artifacts.get(artifactId);
            buildInfo = builds.get(buildId);
        }
        Objects.requireNonNull(ourArtifactImpl, "no such artifact: " + artifact.toString());
        Objects.requireNonNull(buildInfo, "no such build: " + buildId);

        if (ourArtifactImpl != artifact) {
            // Not sure how this can happen...
            throw new IllegalStateException("two artifacts with the same id: " + artifact.toString());
        }

        if (!buildInfo.production().contains(artifactId)) {
            throw new IllegalArgumentException(artifactId + " is not an artifact of build " + buildId.toString());
        }

        return artifactImpl;
    }

    public void printGraphviz(Path dotPath) {
        var graphviz = new Graphviz(Map.copyOf(artifacts), Map.copyOf(builds));
        var string = graphviz.make();
        byte[] utf8bytes = string.toString().getBytes(StandardCharsets.UTF_8);
        uncheckIO(() -> Files.write(dotPath, utf8bytes, StandardOpenOption.TRUNCATE_EXISTING));
    }

    private StringBuilder appendBuildId(StringBuilder builder, BuildId buildId) {
        builder.append(buildId.namespace()).append(':').append(buildId.id());
        return builder;
    }
}
