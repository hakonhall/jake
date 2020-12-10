package no.ion.jake.engine;

import no.ion.jake.LogSink;

public class SingleBuildDriver {
    private final LogSink logSink;

    public SingleBuildDriver(LogSink logSink) {
        this.logSink = logSink;
    }

    public BuildResult runSync(ArtifactRegistry artifactRegistry, BuildInfo info) {
        BuildRunner runner = new BuildRunner(artifactRegistry, info, logSink);
        return runner.runSync();
    }
}
