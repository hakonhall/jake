package no.ion.jake.java;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.util.Java;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JavaArchiver {
    private final List<Artifact<Path>> directoryArtifacts = new ArrayList<>();
    private final Jar jar;
    private final String name;
    private final String mode;

    private Path path = null;
    private String mainClass = null;
    private Artifact<Path> manifestPathArtifact = null;

    public static JavaArchiver forCreatingArchive(Jar jar, String name) {
        return new JavaArchiver(jar, name, "-c");
    }

    private JavaArchiver(Jar jar, String name, String mode) {
        this.jar = jar;
        this.name = name;
        this.mode = mode;
    }

    public JavaArchiver addDirectoryArtifact(Artifact<Path> javaDirectoryArtifact) {
        this.directoryArtifacts.add(javaDirectoryArtifact);
        return this;
    }

    public JavaArchiver addDirectoryArtifacts(List<Artifact<Path>> javaDirectoryArtifacts) {
        this.directoryArtifacts.addAll(javaDirectoryArtifacts);
        return this;
    }

    public JavaArchiver setMainClass(String className) {
        if (!Java.isValidClassName(className)) {
            throw new IllegalArgumentException("not a valid class name: " + className);
        }
        this.mainClass = className;
        return this;
    }

    public JavaArchiver setManifestPathArtifact(Artifact<Path> manifestPath) {
        this.manifestPathArtifact = manifestPath;
        return this;
    }

    public JavaArchiver setOutputFile(Path path) {
        this.path = path;
        return this;
    }

    public Artifact<Path> declareArchive(Declarator declarator) {
        try (var declaration = declarator.declareNewBuild()) {
            directoryArtifacts.forEach(declaration::dependsOn);

            if (manifestPathArtifact != null) {
                declaration.dependsOn(manifestPathArtifact);
            }

            Artifact<Path> jarArtifact = declaration.producesArtifact(Path.class, path.getFileName().toString());
            declaration.forBuild(new JavaArchiverBuild(jar, name, mode, directoryArtifacts, path, mainClass,
                    manifestPathArtifact, jarArtifact));
            return jarArtifact;
        }
    }
}
