package no.ion.jake.engine;

import no.ion.jake.LogSink;
import no.ion.jake.build.Artifact;
import no.ion.jake.build.BuildContext;
import no.ion.jake.build.Logger;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.util.Stopwatch;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class BuildContextImpl implements BuildContext, AutoCloseable {
    private final Set<ArtifactId> publishedArtifacts = new HashSet<>();
    private final ArtifactRegistry artifactRegistry;
    private final Logger logger;
    private final BuildInfo buildInfo;
    private final ModuleContext moduleContext;
    private final String moduleName;
    private final BuildId buildId;
    private final Stopwatch.Running runningStopwatch;

    private boolean closed = false;

    public BuildContextImpl(ArtifactRegistry artifactRegistry, LogSink logSink, BuildInfo buildInfo) {
        this.artifactRegistry = artifactRegistry;
        this.logger = new Logger(logSink, buildInfo.namespace());
        this.buildInfo = buildInfo;
        this.moduleContext = buildInfo.moduleContext();
        this.moduleName = buildInfo.namespace();
        this.buildId = buildInfo.id();
        this.runningStopwatch = Stopwatch.start();
    }

    @Override
    public ModuleContext moduleContext() {
        verifyOpen();
        return moduleContext;
    }

    @Override
    public String namespace() {
        verifyOpen();
        return moduleName;
    }

    @Override
    public Logger log() {
        verifyOpen();
        return logger;
    }

    private class PublicationImpl<T> implements Publication<T> {
        private final Artifact<T> artifact;
        private String accomplishment = null;
        private boolean hasChanged = true;
        private boolean includeTimings = false;

        public PublicationImpl(Artifact<T> artifact) {
            this.artifact = artifact;
        }

        @Override
        public PublicationImpl<T> logWithDuration(String accomplishment) {
            this.includeTimings = true;
            return log(accomplishment);
        }

        @Override
        public PublicationImpl<T> log(String accomplishment) {
            this.accomplishment = accomplishment;
            return this;
        }

        @Override
        public PublicationImpl<T> hasChanged(boolean hasChanged) {
            this.hasChanged = hasChanged;
            return this;
        }

        @Override
        public void publish(T detail) {
            verifyOpen();

            if (accomplishment != null) {
                if (includeTimings) {
                    Duration duration = durationUpToNow();
                    BuildContextImpl.this.log().info("%s in %.3f s", accomplishment, duration.toMillis() / 1000.0);
                } else {
                    BuildContextImpl.this.log().info(accomplishment);
                }
            }

            // todo: handle !hasChanged

            ArtifactImpl<T> artifactImpl;
            try {
                artifactImpl = artifactRegistry.verifyArtifact(artifact, buildId);
            } catch (RuntimeException e) {
                throw new BadBuildException(buildInfo.build(), e);
            }

            if (!publishedArtifacts.add(artifactImpl.artifactId())) {
                throw new IllegalArgumentException("artifact has already been published this build: " + artifactImpl.toString());
            }

            artifactImpl.publish(detail);
        }
    }

    @Override
    public <T> PublicationImpl<T> newPublicationOf(Artifact<T> artifact) {
        return new PublicationImpl<T>(artifact);
    }

    @Override
    public Duration durationUpToNow() {
        // May be invoked both before and after close()
        return runningStopwatch.stop();
    }

    @Override
    public void close() {
        verifyOpen();
        closed = true;
    }

    public Set<ArtifactId> publishedArtifacts() {
        verifyClosed();
        return publishedArtifacts;
    }

    private void verifyOpen() {
        if (closed) {
            throw new IllegalStateException("context has been closed");
        }
    }

    private void verifyClosed() {
        if (!closed) {
            throw new IllegalStateException("context has not been closed");
        }
    }

}
