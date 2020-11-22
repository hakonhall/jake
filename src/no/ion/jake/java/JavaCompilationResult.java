package no.ion.jake.java;

import no.ion.jake.build.BuildResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static java.lang.String.format;

public class JavaCompilationResult implements BuildResult {
    private final int numFilesCompiled;
    private final Optional<String> warning;
    private final Path destinationDirectory;

    public JavaCompilationResult(int numFilesCompiled, String warning, Path destinationDirectory) {
        this.numFilesCompiled = numFilesCompiled;
        this.warning = warning == null || warning.isBlank() ? Optional.empty() : Optional.of(warning);
        this.destinationDirectory = destinationDirectory;

        if (numFilesCompiled < 0) {
            throw new IllegalArgumentException("numFilesCompiled is negative: " + numFilesCompiled);
        }
    }

    @Override
    public boolean noop() {
        return numFilesCompiled == 0;
    }

    @Override
    public String summary() {
        if (numFilesCompiled > 0) {
            return format("compiled %d file%s to %s", numFilesCompiled, numFilesCompiled == 1 ? "" : "s", destinationDirectory);
        } else {
            return "compiled no files";
        }
    }

    public int numFilesCompiled() { return numFilesCompiled; }
    public Optional<String> warning() { return warning; }

    @Override
    public String toString() {
        return "JavaCompilationResult{" +
                ", numFilesCompiled=" + numFilesCompiled +
                ", warning=" + warning +
                '}';
    }
}
