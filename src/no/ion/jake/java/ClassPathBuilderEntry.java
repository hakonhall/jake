package no.ion.jake.java;

import no.ion.jake.JakeException;
import no.ion.jake.maven.MavenArtifact;
import no.ion.jake.maven.Scope;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ClassPathBuilderEntry {
    private final MavenArtifact mavenJarArtifact;
    private final Path explodedJarDirectory;
    private final Scope scope;

    public static ClassPathBuilderEntry fromMavenArtifact(MavenArtifact mavenArtifact, Scope scope) {
        if (!mavenArtifact.packaging().equals("jar")) {
            throw new IllegalArgumentException("Class path can only depend on 'jar' maven artifact: " + mavenArtifact.toString());
        }
        return new ClassPathBuilderEntry(mavenArtifact, null, scope);
    }

    public static ClassPathBuilderEntry fromExplodedJarDirectory(Path explodedJarDirectory, Scope scope) {
        return new ClassPathBuilderEntry(null, explodedJarDirectory, scope);
    }

    private ClassPathBuilderEntry(MavenArtifact mavenJarArtifact, Path explodedJarDirectory, Scope scope) {
        this.mavenJarArtifact = mavenJarArtifact;
        this.explodedJarDirectory = explodedJarDirectory;
        this.scope = scope;
    }

    Optional<MavenArtifact> mavenJarArtifact() { return Optional.ofNullable(mavenJarArtifact); }
    Optional<Path> explodedJarDirectory() { return Optional.ofNullable(explodedJarDirectory); }

    Path resolveToJarPath(Path modulePath, Path mavenRepository) {
        final Path path;
        if (mavenJarArtifact == null) {
            path = explodedJarDirectory.isAbsolute() ?
                    explodedJarDirectory :
                    modulePath.resolve(explodedJarDirectory).normalize();
            if (!Files.isDirectory(path)) {
                throw new JakeException("exploded JAR directory not found: " + path);
            }
        } else {
            path = mavenRepository.resolve(mavenJarArtifact.toRepoPath()).normalize();
            if (!path.toString().endsWith(".jar") || !Files.isRegularFile(path)) {
                throw new JakeException("maven JAR artifact not found: " + path);
            }
        }

        return path;
    }

    Scope scope() { return scope; }

    @Override
    public String toString() {
        if (mavenJarArtifact == null) {
            return explodedJarDirectory.toString();
        } else {
            return mavenJarArtifact.toString();
        }
    }
}
