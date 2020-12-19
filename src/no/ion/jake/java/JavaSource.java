package no.ion.jake.java;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.build.PendingPublication;
import no.ion.jake.io.FileSet2;
import no.ion.jake.io.FileTreeScanner;
import no.ion.jake.io.PathPattern;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a set of directories that <em>may</em> exist, and whose *.java files are considered source files
 * for Java compilation of the Java module.
 */
public class JavaSource {
    private final List<Path> directories = new ArrayList<>();
    private final String name;

    public JavaSource(String name) {
        this.name = name;
    }

    public JavaSource addDirectory(Path directory) {
        this.directories.add(directory);
        return this;
    }

    public JavaSourceArtifacts declareScan(Declarator declarator) {
        // The file scan is delegated to FileTreeScanner
        FileTreeScanner.Builder scanner = FileTreeScanner.newBuilder(declarator, "java files");
        directories.forEach(directory -> scanner.includeFiles(directory, false, PathPattern.of("*.java")));
        Artifact<FileSet2> javaFilesArtifact = scanner.declareScan();

        // The remaining part is just publishing the directories.
        try (var declaration = declarator.declareNewBuild()) {
            declaration.dependsOn(javaFilesArtifact);

            List<PendingPublication<Path>> sourceDirectories = directories.stream()
                    .map(directory -> new PendingPublication<>(declaration.producesArtifact(Path.class, "source directory"), directory))
                    .collect(Collectors.toList());

            declaration.forBuild(new JavaSourceScan(name, sourceDirectories));

            return new JavaSourceArtifacts(javaFilesArtifact, sourceDirectories);
        }
    }
}
