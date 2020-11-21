package no.ion.jake.vespa.osgi;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Manifest {
    private final Map<String, String> entries = new HashMap<>();

    public Manifest() {}

    public Manifest setBundlePathFrom(List<Path> pathsToJarsWithCompileScope) {
        String classPath = Stream.concat(
                Stream.of("."),
                pathsToJarsWithCompileScope.stream().map(path -> "dependencies/" + path.getFileName().toString())
        ).collect(Collectors.joining(","));

        return addEntry("Bundle-ClassPath", classPath);
    }

    public Manifest addEntry(String name, String value) {
        entries.put(name, value);
        return this;
    }
}
