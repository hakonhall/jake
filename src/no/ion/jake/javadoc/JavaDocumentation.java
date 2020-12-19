package no.ion.jake.javadoc;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.java.ClassPathEntry;
import no.ion.jake.maven.MavenArtifactId;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JavaDocumentation {
    private final Javadoc javadoc;
    private final String name;
    private final List<ClassPathEntry> classPath = new ArrayList<>();
    private final List<Artifact<Path>> sourceDirectoryArtifacts = new ArrayList<>();
    private final List<String> extraArguments = new ArrayList<>();
    private Path destinationDirectory = null;
    private MavenArtifactId mavenArtifactId = null;
    private String header = null;
    private String bottom = null;
    private Artifact<Path> classesArtifact = null;

    public JavaDocumentation(Javadoc javadoc, String name) {
        this.javadoc = javadoc;
        this.name = name;
    }

    public JavaDocumentation addClassPathEntries(List<ClassPathEntry> entries) {
        classPath.addAll(entries);
        return this;
    }

    public JavaDocumentation addSourceDirectoryArtifacts(List<Artifact<Path>> sourceDirectoryArtifacts) {
        this.sourceDirectoryArtifacts.addAll(sourceDirectoryArtifacts);
        return this;
    }

    public JavaDocumentation addClassesArtifact(Artifact<Path> classesArtifact) {
        this.classesArtifact = classesArtifact;
        return this;
    }

    public JavaDocumentation addArguments(String... arguments) {
        extraArguments.addAll(List.of(arguments));
        return this;
    }

    public JavaDocumentation setMavenArtifactId(MavenArtifactId mavenArtifactId) {
        this.mavenArtifactId = mavenArtifactId;
        return this;
    }

    public JavaDocumentation setHeader(String header) {
        this.header = header;
        return this;
    }

    public JavaDocumentation setBottom(String bottom) {
        this.bottom = bottom;
        return this;
    }

    public JavaDocumentation setDestinationDirectory(Path destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
        return this;
    }

    public Artifact<Path> declare(Declarator declarator) {
        try (var declaration = declarator.declareNewBuild()) {
            // TODO: Run javadoc in parallel with compilation? If so, need to avoid double-work.
            // sourceDirectoryArtifacts.forEach(declaration::dependsOn);
            // TODO: This is a bit ugly. JavaCompiler should probably have an artifact that exposes class path, classesDirectory, etc.
            // and we could depend on that.
            // Note: classesArtifact is actually not used other than ordering this build after java compile.
            declaration.dependsOn(classesArtifact);
            sourceDirectoryArtifacts.forEach(declaration::dependsOn);
            // TODO: This should output an "optional Path": if no sources are found, no java doc should be generated, and
            // no javadoc jar should be created.
            Artifact<Path> documentation = declaration.producesArtifact(Path.class, "java documentation");

            declaration.forBuild(new JavaDocumentationBuild(name, javadoc, classPath, sourceDirectoryArtifacts, extraArguments,
                    destinationDirectory, mavenArtifactId, header, bottom, documentation));

            return documentation;
        }
    }
}
