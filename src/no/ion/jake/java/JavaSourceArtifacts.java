package no.ion.jake.java;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.PendingPublication;
import no.ion.jake.io.FileSet2;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class JavaSourceArtifacts {
    private final Artifact<FileSet2> javaFiles;
    private final List<Artifact<Path>> sourceDirectories;

    public JavaSourceArtifacts(Artifact<FileSet2> javaFiles, List<PendingPublication<Path>> sourceDirectories) {
        this.javaFiles = javaFiles;
        this.sourceDirectories = sourceDirectories.stream()
                .map(PendingPublication::artifact)
                .collect(Collectors.toList());
    }

    public Artifact<FileSet2> javaFilesArtifact() {
        return javaFiles;
    }

    /**
     * Returns a list of artifacts of the directories containing *.java files. They may not exist, and may not contain
     * any java files.
     */
    public List<Artifact<Path>> javaDirectoryArtifacts() {
        return sourceDirectories;
    }
}
