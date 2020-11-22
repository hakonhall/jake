package no.ion.jake.java;

import no.ion.jake.build.BuildResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public class JavaArchiverResult implements BuildResult {
    private final String warning;
    private final Path path;

    public JavaArchiverResult(String warning, Path path) {
        this.warning = warning;
        this.path = path;
    }

    public Optional<String> warning() {
        return warning.isEmpty() ? Optional.empty() : Optional.of(warning);
    }

    @Override
    public String summary() {
        return "archived " + path;
    }
}
