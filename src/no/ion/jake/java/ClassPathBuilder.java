package no.ion.jake.java;

import no.ion.jake.module.ModuleContext;
import no.ion.jake.maven.MavenArtifact;
import no.ion.jake.maven.Scope;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains declared dependencies on maven JAR artifacts and exploded JAR directories at particular Maven scopes.
 * These may not be immediately available, but once they are, this class is able to verify and produce a class
 * path.
 */
public class ClassPathBuilder {
    public static final EnumSet<Scope> COMPILE_SCOPES = EnumSet.of(Scope.PROVIDED, Scope.COMPILE);
    public static final EnumSet<Scope> TEST_SCOPES = EnumSet.of(Scope.PROVIDED, Scope.COMPILE, Scope.TEST);
    private final List<ClassPathBuilderEntry> classPath = new ArrayList<>();
    private final Path modulePath;
    private final Path localMavenRepository;
    private Scope defaultScope = Scope.COMPILE;
    private Path compileDestinationDirectory = null;
    private Path testCompileDestinationDirectory = null;

    public ClassPathBuilder(ModuleContext moduleContext) {
        this.modulePath = moduleContext.path();
        this.localMavenRepository = moduleContext.project().pathToMavenRepository();

        if (!localMavenRepository.isAbsolute()) {
            throw new IllegalArgumentException("maven repository path must be absolute: " + localMavenRepository);
        }
    }

    public boolean isEmpty() { return classPath.isEmpty(); }

    public ClassPathBuilder setCompileDestinationDirectory(String path) {
        return setCompileDestinationDirectory(modulePath.getFileSystem().getPath(path));
    }

    public ClassPathBuilder setCompileDestinationDirectory(Path compileDestinationDirectory) {
        this.compileDestinationDirectory = compileDestinationDirectory;
        return this;
    }

    public Path getCompileDestinationDirectory() { return compileDestinationDirectory; }

    private Path getResolvedCompileDestinationDirectory() {
        return compileDestinationDirectory.isAbsolute() ?
                compileDestinationDirectory :
                modulePath.resolve(compileDestinationDirectory);
    }

    public ClassPathBuilder setTestCompileDestinationDirectory(String path) {
        return setTestCompileDestinationDirectory(modulePath.getFileSystem().getPath(path));
    }

    public ClassPathBuilder setTestCompileDestinationDirectory(Path testCompileDestinationDirectory) {
        this.testCompileDestinationDirectory = testCompileDestinationDirectory;
        return this;
    }

    public Path getTestCompilationDestinationDirectory() {
        if (testCompileDestinationDirectory == null) {
            throw new NullPointerException("testCompileDestinationDirectory is null");
        }
        return testCompileDestinationDirectory;
    }

    /** "maven" because this may have to return non-jar artifacts like "pom" (tbd) */
    public List<MavenArtifact> getMavenDependencies(Set<Scope> scopesToInclude) {
        return classPath.stream()
                .filter(entry -> entry.mavenJarArtifact().isPresent() && scopesToInclude.contains(entry.mavenJarArtifact().orElseThrow().scope()))
                .map(entry -> entry.mavenJarArtifact().orElseThrow())
                .collect(Collectors.toList());
    }

    /** Set the scope to use on following calls to e.g. {@link #addMavenJar(String) addMavenJar()} when no explicit scope is given. */
    public ClassPathBuilder setDefaultScope(Scope scope) {
        defaultScope = scope;
        return this;
    }

    public ClassPathBuilder addMavenJar(String mavenCoordinate) { return addMavenJar(mavenCoordinate, defaultScope); }
    public ClassPathBuilder addMavenJar(String coordinate, Scope scope) {
        return addMavenJarArtifact(MavenArtifact.fromCoordinate(coordinate), scope);
    }

    public ClassPathBuilder addMavenJarArtifact(MavenArtifact mavenArtifact, Scope scope) {
        if (!mavenArtifact.packaging().equals("jar")) {
            throw new IllegalArgumentException("Class path can only depend on 'jar' maven artifact: " + mavenArtifact.toString());
        }
        classPath.add(ClassPathBuilderEntry.fromMavenArtifact(mavenArtifact.withScope(scope), scope));
        return this;
    }

    /** A relative path is relative the module directory. */
    public ClassPathBuilder addExplodedJar(Path directory) { return addExplodedJar(directory, defaultScope); }

    /** A relative path is relative the module directory. */
    public ClassPathBuilder addExplodedJar(Path directory, Scope scope) {
        if (!directory.isAbsolute()) {
            directory = modulePath.resolve(directory);
        }
        classPath.add(ClassPathBuilderEntry.fromExplodedJarDirectory(directory, scope));
        return this;
    }

    public ClassPathBuilder addModuleJar(JavaModule module) { return addModuleJar(module, defaultScope); }
    public ClassPathBuilder addModuleJar(JavaModule module, Scope scope) {
        return addMavenJarArtifact(module.jarMavenArtifact(), scope);
    }

    public ClassPathBuilder addFrom(ClassPathBuilder that) {
        classPath.addAll(that.classPath);
        return this;
    }

    public ClassPath toClassPathForCompile() {
        var compileClassPath = new ClassPath();
        this.classPath.stream()
                .filter(entry -> COMPILE_SCOPES.contains(entry.scope()))
                .map(entry -> entry.resolveToJarPath(modulePath, localMavenRepository))
                .forEach(compileClassPath::addJar);
        return compileClassPath;
    }

    public ClassPath toClassPathForTestCompile() {
        var testCompileClassPath = new ClassPath();
        this.classPath.stream()
                .filter(entry -> TEST_SCOPES.contains(entry.scope()))
                .map(entry -> entry.resolveToJarPath(modulePath, localMavenRepository))
                .forEach(testCompileClassPath::addJar);
        if (compileDestinationDirectory != null) {
            testCompileClassPath.addExplodedJar(getResolvedCompileDestinationDirectory());
        }
        return testCompileClassPath;
    }

    public ClassPath toClassPathForTest() {
        ClassPath classPath = new ClassPath();

        if (compileDestinationDirectory == null) {
            throw new IllegalStateException("compile destination directory not set");
        }
        classPath.addExplodedJar(getResolvedCompileDestinationDirectory());

        if (testCompileDestinationDirectory == null) {
            throw new IllegalStateException("test compile destination directory not set");
        }
        classPath.addExplodedJar(modulePath.resolve(testCompileDestinationDirectory));

        this.classPath.stream().forEach(entry -> {
            entry.explodedJarDirectory().ifPresent(classPath::addExplodedJar);
            entry.mavenJarArtifact().ifPresent(mavenJarArtifact -> classPath.addJar(entry.resolveToJarPath(modulePath, localMavenRepository)));
        });

        return classPath;
    }

    public List<Path> getPathsToJarsWithScope(Scope scope) {
        return classPath.stream()
                .filter(entry -> entry.mavenJarArtifact().isPresent())
                .map(entry -> localMavenRepository.resolve(entry.mavenJarArtifact().orElseThrow().toRepoPath()))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("toString()");
    }
}
