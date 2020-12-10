package no.ion.jake.engine;

import no.ion.jake.LogSink;

public class BuildRunner {
    private final ArtifactRegistry artifactRegistry;
    private final BuildInfo buildInfo;
    private final LogSink logSink;

    public BuildRunner(ArtifactRegistry artifactRegistry, BuildInfo buildInfo, LogSink logSink) {
        this.artifactRegistry = artifactRegistry;
        this.buildInfo = buildInfo;
        this.logSink = logSink;
    }

    public BuildResult runSync() {
        var context = new BuildContextImpl(artifactRegistry, logSink, buildInfo);
        try (context) {
            buildInfo.build().build(context);
        } catch (RuntimeException exception) {
            return BuildResult.fromException(context, exception);
        } catch (Error error) {
            return BuildResult.fromError(context, error);
        }

        return BuildResult.fromSuccess(context);
    }
}
