package no.ion.jake.java;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class JavaArchiverBuild implements Build {
    private final Jar jar;
    private final String name;
    private final String mode;
    private final List<Artifact<Path>> includeDirectoryArtifacts;
    private final Path jarPath;
    private final String mainClass;
    private final Artifact<Path> manifestPathArtifact;
    private final Artifact<Path> jarArtifact;

    public JavaArchiverBuild(Jar jar, String name, String mode, List<Artifact<Path>> includeDirectoryArtifacts, Path jarPath,
                             String mainClass, Artifact<Path> manifestPathArtifact, Artifact<Path> jarArtifact) {
        this.jar = Objects.requireNonNull(jar);
        this.name = name;
        this.mode = Objects.requireNonNull(mode);
        this.includeDirectoryArtifacts = includeDirectoryArtifacts;
        this.jarPath = Objects.requireNonNull(jarPath);
        this.mainClass = mainClass;
        this.manifestPathArtifact = manifestPathArtifact;
        this.jarArtifact = Objects.requireNonNull(jarArtifact);
    }

    @Override public String name() { return name; }

    @Override
    public void build(BuildContext buildContext) {
        var jarArguments = new ArrayList<String>();
        jarArguments.add(mode);

        Path resolvedJarPath = buildContext.moduleContext().resolve(jarPath);
        uncheckIO(() -> Files.createDirectories(resolvedJarPath.getParent()));
        jarArguments.add("-f");
        jarArguments.add(resolvedJarPath.toString());

        if (mainClass != null) {
            jarArguments.add("-e");
            jarArguments.add(mainClass);
        }

        if (manifestPathArtifact != null) {
            Path resolvedManifestPath = buildContext.moduleContext().resolve(manifestPathArtifact.detail());
            if (!Files.isRegularFile(resolvedManifestPath)) {
                throw new UncheckedIOException(new NoSuchFileException(resolvedManifestPath.toString()));
            }
            jarArguments.add("-m");
            jarArguments.add(resolvedManifestPath.toString());
        }

        includeDirectoryArtifacts.forEach(includeDirectoryArtifact -> {
            Path includeDirectory = buildContext.moduleContext().resolve(includeDirectoryArtifact.detail());
            if (!Files.isDirectory(includeDirectory)) {
                throw new UncheckedIOException(new NoSuchFileException("no such directory: " + includeDirectory));
            }

            jarArguments.add("-C");
            jarArguments.add(includeDirectory.toString());
            jarArguments.add(".");
        });

        buildContext.log().debug(() -> "jar " + String.join(" ", jarArguments));

        Jar.Result result = jar.run(jarArguments);

        if (result.code() != 0) {
            throw new JavaArchiverException(result.out());
        }

        String warning = result.out();
        if (!warning.isEmpty()) {
            buildContext.log().warning(warning);
        }

        buildContext.newPublicationOf(jarArtifact)
                .logWithDuration("archived " + jarPath)
                .publish(jarPath);
    }
}
