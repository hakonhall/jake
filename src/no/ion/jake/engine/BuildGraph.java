package no.ion.jake.engine;

import no.ion.jake.LogSink;
import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.Module;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.util.SetUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BuildGraph {
    private final Object monitor = new Object();
    private final HashMap<ArtifactId, ArtifactImpl<?>> artifacts = new HashMap<>();
    private final HashMap<BuildId, BuildInfo> builds = new HashMap<>();
    private final List<BuildInfo> buildInfoList = new ArrayList<>();
    private final Deque<ResultInfo> results = new ArrayDeque<>();
    private final JakeExecutor jakeExecutor;
    private final LogSink logSink;
    private final SingleBuildDriver singleBuildDriver;

    public BuildGraph(JakeExecutor jakeExecutor, LogSink logSink, SingleBuildDriver singleBuildDriver) {
        this.jakeExecutor = jakeExecutor;
        this.logSink = logSink;
        this.singleBuildDriver = singleBuildDriver;
    }

    public <T> ArtifactImpl<T> newArtifact(Class<T> artifactClass, String moduleNameOrNull, String name) {
        var artifactId = new ArtifactId(moduleNameOrNull, name);
        var artifact = new ArtifactImpl<>(artifactId, artifactClass);

        synchronized (monitor) {
            if (artifacts.put(artifactId, artifact) != null) {
                throw new IllegalArgumentException("duplicate artifact: " + artifactId.toString());
            }
        }

        return artifact;
    }

    public void addBuild(ModuleContext moduleContext, Module module, Build build, List<Artifact<?>> dependencies,
                         Set<ArtifactImpl<?>> production) {
        Set<ArtifactId> dependencySet = dependencies.stream()
                .map(this::verifyArtifact)
                .map(ArtifactImpl::artifactId)
                .collect(Collectors.toSet());

        Set<ArtifactId> productionSet = production.stream()
                .map(ArtifactImpl::artifactId)
                .collect(Collectors.toSet());

        BuildId buildId = new BuildId(module.moduleName(), build.name());
        var info = new BuildInfo(buildId, moduleContext, module, build, dependencySet, productionSet);
        synchronized (monitor) {
            if (!artifacts.keySet().containsAll(dependencySet)) {
                throw new IllegalArgumentException(buildId.toString() + " depends on artifacts that no-one are producing");
            }

            if (builds.putIfAbsent(buildId, info) != null) {
                throw new IllegalArgumentException("duplicate build: " + buildId.toString());
            }

            buildInfoList.add(info);
        }
    }

    private static class ResultInfo {
        private final BuildInfo buildInfo;
        private final BuildResult result;

        public ResultInfo(BuildInfo buildInfo, BuildResult result) {
            this.buildInfo = buildInfo;
            this.result = result;
        }
    }

    public void buildEverything() {
        Set<BuildId> pendingBuilds = new HashSet<>(builds.keySet());
        Set<BuildId> activeBuilds = new HashSet<>();
        Set<ArtifactId> completedArtifacts = new HashSet<>();

        synchronized (monitor) {
            do {
                do {
                    Optional<BuildInfo> buildReadyToRun = pendingBuilds.stream()
                            .map(buildId -> {
                                BuildInfo info = getBuildInfo(buildId);
                                for (var dependency : info.dependencies()) {
                                    if (!completedArtifacts.contains(dependency)) {
                                        return null;
                                    }
                                }

                                return info;
                            })
                            .filter(Objects::nonNull)
                            .findAny();
                    if (buildReadyToRun.isEmpty()) {
                        break;
                    }

                    logSink.log(Level.FINE, "Scheduling build of '" + buildReadyToRun.get().id() + "'", null);

                    pendingBuilds.remove(buildReadyToRun.get().id());
                    activeBuilds.add(buildReadyToRun.get().id());

                    jakeExecutor.runAsync(() -> {
                        BuildResult result = singleBuildDriver.runSync(this::verifyArtifact, buildReadyToRun.get());

                        synchronized (monitor) {
                            results.addLast(new ResultInfo(buildReadyToRun.get(), result));
                            monitor.notifyAll();
                        }
                    });
                } while (true);

                while (!results.isEmpty()) {
                    ResultInfo resultInfo = results.pollFirst();
                    BuildResult result = resultInfo.result;
                    BuildInfo completedBuild = resultInfo.buildInfo;

                    activeBuilds.remove(completedBuild.id());

                    result.getRuntimeException().ifPresent(e -> {
                        throw e;
                    });
                    result.getError().ifPresent(e -> {
                        throw e;
                    });

                    Set<ArtifactId> declaredArtifacts = completedBuild.production();
                    Set<ArtifactId> publishedArtifacts = result.publishedArtifacts();

                    Set<ArtifactId> unpublishedArtifacts = SetUtil.difference(declaredArtifacts, publishedArtifacts);
                    if (!unpublishedArtifacts.isEmpty()) {
                        throw new BadBuildException(completedBuild.build(), "failed to publish: " +
                                unpublishedArtifacts.stream().map(ArtifactId::artifactName).collect(Collectors.joining(", ")));
                    }

                    Set<ArtifactId> unknownArtifacts = SetUtil.difference(publishedArtifacts, declaredArtifacts);
                    if (!unknownArtifacts.isEmpty()) {
                        // this is impossible at the time this comment was made
                        throw new BadBuildException(completedBuild.build(), "published extraneous artifacts: " +
                                unknownArtifacts.stream().map(ArtifactId::artifactName).collect(Collectors.joining(", ")));
                    }

                    completedArtifacts.addAll(publishedArtifacts);
                }

                if (pendingBuilds.isEmpty() && activeBuilds.isEmpty()) {
                    break;
                }

                try {
                    monitor.wait(100);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            } while (true);
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

    private BuildInfo getBuildInfo(BuildId buildId) {
        synchronized (monitor) {
            return Objects.requireNonNull(builds.get(buildId), "no build is associated with ID " + buildId.toString());
        }
    }
}
