package no.ion.jake.vespa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Options {
    private Path dotPath = null;
    private Path jarPath = null;
    private boolean logTime = false;
    private Mode mode = Mode.BUILD;
    private final int processorCount = Runtime.getRuntime().availableProcessors();
    private Path projectPath = Path.of(".");
    private float threads = processorCount;
    private boolean verbose = false;

    public enum Mode { BUILD, GRAPHVIZ }
    public void setDotPath(Path dotPath) {
        this.dotPath = Objects.requireNonNull(dotPath);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setJarPath(Path jarPath) {
        this.jarPath = jarPath;
    }


    public void setProjectPath(Path projectPath) {
        this.projectPath = projectPath;
    }

    public void setThreadsPerHardwareThread(float threadsPerHardwareThread) {
        this.threads = threadsPerHardwareThread * processorCount;
    }

    public void setThreads(float threads) {
        this.threads = threads;
    }

    public void setLogTime(boolean logTime) {
        this.logTime = logTime;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void validateAndNormalize() {
        if (jarPath == null) {
            throw new UserError("missing --jar");
        } else if (!jarPath.toString().endsWith(".jar")) {
            throw new UserError("--jar file does not end in .jar: " + jarPath);
        } else if (!Files.isRegularFile(jarPath)) {
            throw new UserError("no such file: " + jarPath);
        }

        if (dotPath != null) {
            if (!Files.isDirectory(dotPath.getParent())) {
                throw new UserError("parent directory of dot file does not exist: " + dotPath);
            }
        }

        threads = Math.max(1f, threads);

        if (!Files.isDirectory(projectPath)) {
            throw new UserError("no such directory: " + projectPath);
        }
        projectPath = projectPath.toAbsolutePath();
    }

    public Path dotPath() { return dotPath; }
    public Path jarPath() { return jarPath; }
    public boolean logTime() { return logTime; }
    public Mode mode() { return mode; }
    public int processorCount() { return processorCount; }
    public Path projectPath() { return projectPath; }
    public float threads() { return threads; }
    public boolean verbose() { return verbose; }
}
