package no.ion.jake;

import no.ion.jake.maven.MavenArtifact;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Objects;

public class Project {
    private final FileSystem fileSystem;
    private final Path path;
    private final Path home;
    private final Path localRepo;

    public Project(Path path) {
        this.fileSystem = path.getFileSystem();
        this.path = path;

        String home = System.getProperty("user.home");
        if (home == null) {
            throw new UserError("system property not set: user.home");
        }
        this.home = fileSystem.getPath(home);
        this.localRepo = this.home.resolve(".m2/repository");
    }

    public FileSystem fileSystem() {
        return fileSystem;
    }

    public Path path() {
        return path;
    }

    public Path pathToMavenArtifactInLocalRepo(MavenArtifact artifactId) {
        String relativePath = artifactId.toRepoPath();
        return localRepo.resolve(relativePath);
    }

    public Path pathToMavenRepository() {
        return localRepo;
    }

    public Path pathToMavenArtifactInLocalRepo(String groupId, String artifactId, String version, String classifier) {
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(artifactId);
        Objects.requireNonNull(version);
        // classifier may be null

        Path path = localRepo;
        for (String group : groupId.split("\\.", -1)) {
            validatePathComponent(group, "groupId");
            path = path.resolve(group);
        }

        validatePathComponent(artifactId, "artifactId");
        path = path.resolve(artifactId);

        validatePathComponent(version, "version");
        path = path.resolve(version);

        String filename = String.format("%s-%s%s.jar",
                artifactId,
                version,
                classifier == null ? "" : "-" + classifier);

        validatePathComponent(filename, "artifact filename");
        return path.resolve(filename);
    }

    public Path pathToMavenArtifactInLocalRepo(String groupId, String artifactId, String version) {
        return pathToMavenArtifactInLocalRepo(groupId, artifactId, version, null);
    }

    public Path pathToMavenArtifactInLocalRepo(String mavenCoordinate) {
        String[] elements = mavenCoordinate.split(":", -1);
        switch (elements.length) {
            case 3: return pathToMavenArtifactInLocalRepo(elements[0], elements[1], elements[2]);
            case 4: return pathToMavenArtifactInLocalRepo(elements[0], elements[1], elements[2], elements[3]);
            default: throw new UserError("bad maven coordinate: " + mavenCoordinate);
        }
    }

    private static void validatePathComponent(String component, String name) {
        if (component.indexOf('/') != -1 || component.isEmpty() || component.equals(".") || component.equals("..")) {
            throw new UserError("bad " + name + ": " + component);
        }
    }
}
