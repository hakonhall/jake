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
import java.util.logging.Level;
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
                        switch (buildInfo.id().toString()) {
                            case "check abi in module vespajlib": return Duration.ofMillis(51);
                            case "check abi in module yolean": return Duration.ofMillis(45);
                            case "downloading com.fasterxml.jackson.core:jackson-annotations:2.8.11 in module maven": return Duration.ofMillis(0);
                            case "downloading com.fasterxml.jackson.core:jackson-core:2.8.11 in module maven": return Duration.ofMillis(0);
                            case "downloading com.fasterxml.jackson.core:jackson-databind:2.8.11.6 in module maven": return Duration.ofMillis(0);
                            case "downloading com.google.guava:guava:20.0 in module maven": return Duration.ofMillis(0);
                            case "downloading com.google.inject:guice:3.0:no_aop in module maven": return Duration.ofMillis(0);
                            case "downloading com.google.jimfs:jimfs:1.1 in module maven": return Duration.ofMillis(0);
                            case "downloading com.yahoo.vespa:annotations:7-SNAPSHOT in module maven": return Duration.ofMillis(0);
                            case "downloading junit:junit:4.12 in module maven": return Duration.ofMillis(0);
                            case "downloading net.bytebuddy:byte-buddy-agent:1.9.10 in module maven": return Duration.ofMillis(0);
                            case "downloading net.bytebuddy:byte-buddy:1.9.10 in module maven": return Duration.ofMillis(0);
                            case "downloading net.java.dev.jna:jna:4.5.2 in module maven": return Duration.ofMillis(0);
                            case "downloading org.apache.commons:commons-exec:1.3 in module maven": return Duration.ofMillis(0);
                            case "downloading org.hamcrest:hamcrest-core:1.3 in module maven": return Duration.ofMillis(0);
                            case "downloading org.hamcrest:hamcrest-library:1.3 in module maven": return Duration.ofMillis(0);
                            case "downloading org.lz4:lz4-java:1.7.1 in module maven": return Duration.ofMillis(0);
                            case "downloading org.mockito:mockito-core:3.1.0 in module maven": return Duration.ofMillis(0);
                            case "downloading org.objenesis:objenesis:2.6 in module maven": return Duration.ofMillis(0);
                            case "downloading uk.co.datumedge:hamcrest-json:0.2 in module maven": return Duration.ofMillis(0);
                            case "finding java source in module yolean": return Duration.ofMillis(14);
                            case "finding java source in module testutil": return Duration.ofMillis(14);
                            case "finding java source in module vespajlib": return Duration.ofMillis(20);
                            case "finding test source in module yolean": return Duration.ofMillis(14);
                            case "finding test source in module testutil": return Duration.ofMillis(14);
                            case "finding test source in module vespajlib": return Duration.ofMillis(18);
                            case "generate bundle classpath mappings file in module vespajlib": return Duration.ofMillis(0);
                            case "generate bundle classpath mappings file in module yolean": return Duration.ofMillis(141);
                            case "generate javadoc in module testutil": return Duration.ofMillis(510);
                            case "generate javadoc in module vespajlib": return Duration.ofMillis(1506);
                            case "generate javadoc in module yolean": return Duration.ofMillis(391);
                            case "install of com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT-javadoc.jar in module testutil": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT-sources.jar in module testutil": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT.jar in module testutil": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/testutil/7-SNAPSHOT/testutil-7-SNAPSHOT.pom in module testutil": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT-javadoc.jar in module vespajlib": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT-sources.jar in module vespajlib": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT.jar in module vespajlib": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/vespajlib/7-SNAPSHOT/vespajlib-7-SNAPSHOT.pom in module vespajlib": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT-javadoc.jar in module yolean": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT-sources.jar in module yolean": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT.jar in module yolean": return Duration.ofMillis(10);
                            case "install of com/yahoo/vespa/yolean/7-SNAPSHOT/yolean-7-SNAPSHOT.pom in module yolean": return Duration.ofMillis(10);
                            case "jar in module vespajlib": return Duration.ofMillis(82);
                            case "jar in module yolean": return Duration.ofMillis(5);
                            case "jar with dependencies in module vespajlib": return Duration.ofMillis(50);
                            case "jar with dependencies in module yolean": return Duration.ofMillis(5);
                            case "java archive in module testutil": return Duration.ofMillis(2);
                            case "javadoc archive in module testutil": return Duration.ofMillis(28);
                            case "javadoc archive in module vespajlib": return Duration.ofMillis(60);
                            case "javadoc archive in module yolean": return Duration.ofMillis(29);
                            case "osgi manifest in module vespajlib": return Duration.ofMillis(110);
                            case "osgi manifest in module yolean": return Duration.ofMillis(322);
                            case "source archive in module testutil": return Duration.ofMillis(28);
                            case "source archive in module vespajlib": return Duration.ofMillis(55);
                            case "source archive in module yolean": return Duration.ofMillis(29);
                            case "source compilation in module testutil": return Duration.ofMillis(469);
                            case "source compilation in module vespajlib": return Duration.ofMillis(1216);
                            case "source compilation in module yolean": return Duration.ofMillis(627);
                            case "source in module testutil": return Duration.ofMillis(0);
                            case "source in module vespajlib": return Duration.ofMillis(0);
                            case "source in module yolean": return Duration.ofMillis(0);
                            case "test run in module testutil": return Duration.ofMillis(9);
                            case "test run in module vespajlib": return Duration.ofMillis(1961);
                            case "test run in module yolean": return Duration.ofMillis(230);
                            case "test source compilation in module testutil": return Duration.ofMillis(73);
                            case "test source compilation in module vespajlib": return Duration.ofMillis(1243);
                            case "test source compilation in module yolean": return Duration.ofMillis(356);
                        }

                        throw new IllegalArgumentException("duration not known for: " + buildInfo.id().toString());
                        // TODO: Get this from earlier runs
                        // return Duration.ofMillis(100);
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
