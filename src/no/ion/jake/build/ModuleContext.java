package no.ion.jake.build;

import no.ion.jake.Project;

import java.nio.file.FileSystem;
import java.nio.file.Path;

public class ModuleContext {
    private final Project project;
    private final Path path;

    public ModuleContext(Project project, Path path) {
        this.project = project;
        this.path = path;
    }

    public Path path() { return path; }
    public FileSystem fileSystem() { return path.getFileSystem(); }
    public Path pathOf(String first, String... more) { return fileSystem().getPath(first, more).normalize(); }
    public Path resolve(String path) { return this.path.resolve(path).normalize(); }
    public Path resolve(Path path) { return this.path.resolve(path).normalize(); }

    public Path jakeJarPath() { return project.jakeJarPath(); }

    public Project getProject() {
        // TODO: REMOVE
        return project;
    }
}
