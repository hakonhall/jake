package no.ion.jake.java;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.OptionalArtifact;
import no.ion.jake.maven.MavenArtifact;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

public interface ClassPathEntry {
    Optional<Artifact<?>> getArtifact();
    Path getValidatedPath();

    enum Type { JAR, EXPLODED_JAR }
    Type getType();

    static ClassPathEntry fromMavenArtifact(MavenArtifact mavenArtifact) {
        return fromJarPathArtifact(mavenArtifact.pathArtifact());
    }

    static ClassPathEntry fromJar(Path jarPath) {
        verifyJar(jarPath);

        return new ClassPathEntry() {
            @Override public Optional<Artifact<?>> getArtifact() { return Optional.empty(); }
            @Override public Path getValidatedPath() { return jarPath; }
            @Override public Type getType() { return Type.JAR; }
        };
    }

    static ClassPathEntry fromOptionalJarArtifact(OptionalArtifact<Path> jarPathArtifact) {
        return new ClassPathEntry() {
            @Override public Optional<Artifact<?>> getArtifact() { return jarPathArtifact.getArtifact(); }
            @Override public Path getValidatedPath() {
                Path path = jarPathArtifact.get();
                return ClassPathEntry.verifyJar(path);
            }
            @Override public Type getType() { return Type.JAR; }
        };
    }

    static ClassPathEntry fromJarPathArtifact(Artifact<Path> jarArtifact) {
        return new ClassPathEntry() {
            @Override public Optional<Artifact<?>> getArtifact() { return Optional.of(jarArtifact); }
            @Override public Path getValidatedPath() {
                Path path = jarArtifact.detail();
                return ClassPathEntry.verifyJar(path);
            }
            @Override public Type getType() { return Type.JAR; }
        };
    }

    static ClassPathEntry fromExplodedJar(Path directory) {
        verifyExplodedJar(directory);

        return new ClassPathEntry() {
            @Override public Optional<Artifact<?>> getArtifact() { return Optional.empty(); }
            @Override public Path getValidatedPath() { return directory; }
            @Override public Type getType() { return Type.EXPLODED_JAR; }
        };
    }

    static ClassPathEntry fromExplodedJarArtifact(Artifact<Path> explodedJarArtifact) {
        return new ClassPathEntry() {
            @Override public Optional<Artifact<?>> getArtifact() { return Optional.of(explodedJarArtifact); }
            @Override public Path getValidatedPath() { return verifyExplodedJar(explodedJarArtifact.detail()); }
            @Override public Type getType() { return Type.EXPLODED_JAR; }
        };
    }

    private static Path verifyJar(Path path) {
        if (!path.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException("JAR file must have 'jar' extension: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new UncheckedIOException(new NoSuchFileException(path.toString()));
        }
        return path;
    }

    private static Path verifyExplodedJar(Path directory) {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("is not an exploded JAR directory: " + directory);
        }
        return directory;
    }
}
