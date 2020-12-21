package no.ion.jake.java;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.io.FileSet2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JavaCompiler {
    private final Javac javac;
    private final String nameOrNull;
    private final List<Artifact<FileSet2>> sourceFilesArtifacts = new ArrayList<>();
    private final List<String> passthroughJavacArguments = new ArrayList<>();
    private final List<ClassPathEntry> classPath = new ArrayList<>();
    private Path destinationDirectory;

    private boolean declareCompileCalled = false;

    public JavaCompiler(Javac javac, String nameOrNull) {
        this.javac = javac;
        this.nameOrNull = nameOrNull;
    }

    public JavaCompiler addSourceFilesArtifact(Artifact<FileSet2> sourceFiles) {
        sourceFilesArtifacts.add(sourceFiles);
        return this;
    }

    public JavaCompiler addJavacArguments(List<String> arguments) {
        passthroughJavacArguments.addAll(arguments);
        return this;
    }

    public JavaCompiler addClassPathEntry(ClassPathEntry entry) {
        classPath.add(entry);
        return this;
    }

    public JavaCompiler addClassPathEntries(List<ClassPathEntry> entries) {
        classPath.addAll(entries);
        return this;
    }

    public JavaCompiler setDestinationDirectory(Path destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
        return this;
    }

    public Artifact<Path> declareCompile(Declarator declarator) {
        if (declareCompileCalled) {
            throw new IllegalStateException("declareCompile() can only be invoked once");
        }
        declareCompileCalled = true;

        try (Declarator.BuildDeclaration compilation = declarator.declareNewBuild()) {
            sourceFilesArtifacts.forEach(compilation::dependsOn);
            classPath.forEach(entry -> entry.getArtifact().ifPresent(compilation::dependsOn));
            String artifactNamePrefix = (nameOrNull == null || nameOrNull.isEmpty()) ? "" : nameOrNull + " ";
            Artifact<Path> destinationDirectoryArtifact = compilation.producesArtifact(Path.class, artifactNamePrefix + "classes");
            compilation.forBuild(new JavaCompilerBuild(javac, nameOrNull, sourceFilesArtifacts, passthroughJavacArguments, classPath,
                    destinationDirectory, destinationDirectoryArtifact));
            return destinationDirectoryArtifact;
        }
    }
}
