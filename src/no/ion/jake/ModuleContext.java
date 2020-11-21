package no.ion.jake;

import no.ion.jake.io.FileSet;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;
import java.util.Objects;

public class ModuleContext {
    private final Project project;
    private final Path relativePath;
    private final Path path;
    private final String name;
    private final Version version;

    private MavenArtifact artifactId;

    public ModuleContext(Project project, String relativePath) {
        this(project, relativePath, Path.of(relativePath).getFileName().toString(), Version.getDefault());
    }

    /**
     * @param project      the project
     * @param relativePath path relative to {@link Project#path()}, to the module.  Could be e.g. ".".
     * @param name         the module name, aka artifactId in pom.xml.  The name in pom.xml most often maps to
     *                     its artifactId (for Vespa), but can be e.g. "Vespa log".
     */
    public ModuleContext(Project project, String relativePath, String name, Version version) {
        this.project = project;
        this.name = name;
        this.version = version;

        // Validate relativePath is actually within project, e.g. not ../foo or /bar.
        Path relativePath2 = project.fileSystem().getPath(relativePath);
        if (relativePath2.isAbsolute()) {
            throw new IllegalArgumentException("relativePath cannot be absolute: " + relativePath);
        }
        this.path = project.path().resolve(relativePath2).normalize();
        Path recalculatedRelativePath = project.path().relativize(path).normalize();
        if (recalculatedRelativePath.toString().isEmpty()) {
            this.relativePath = project.fileSystem().getPath(".");
        } else {
            this.relativePath = recalculatedRelativePath;
        }

        setArtifactId(MavenArtifact.from("com.yahoo.vespa", name));
    }

    public String name() { return name; }
    public Version version() { return version; }
    public Path path() { return path; }
    public Path relativePath() { return relativePath; }
    public FileSet newFileSet() { return new FileSet(path); }
    public Project project() { return project; }
    public MavenArtifact mavenArtifact() { return Objects.requireNonNull(artifactId, "artifactId of module not set"); }

    private ModuleContext setArtifactId(MavenArtifact artifactId) {
        this.artifactId = artifactId.optionalVersion()
                .map(version -> artifactId)
                .orElseGet(() -> artifactId.withVersion("7-SNAPSHOT"));
        return this;
    }
}
