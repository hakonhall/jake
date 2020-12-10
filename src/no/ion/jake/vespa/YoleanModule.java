package no.ion.jake.vespa;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.build.JavaModule;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.io.ArtifactInstaller;
import no.ion.jake.io.FileSet2;
import no.ion.jake.io.FileTreeScanner;
import no.ion.jake.io.PathPattern;
import no.ion.jake.java.ClassPathEntry;
import no.ion.jake.java.Jar;
import no.ion.jake.java.JavaArchiver;
import no.ion.jake.java.JavaCompiler;
import no.ion.jake.java.JavaSource;
import no.ion.jake.java.JavaSourceArtifacts;
import no.ion.jake.java.Javac;
import no.ion.jake.javadoc.JavaDocumentation;
import no.ion.jake.javadoc.Javadoc;
import no.ion.jake.junit4.JUnit4TestRunDeclaration;
import no.ion.jake.maven.MavenArtifact;
import no.ion.jake.maven.MavenArtifactId;
import no.ion.jake.maven.MavenRepository;
import no.ion.jake.vespa.abiCheckPlugin.AbiChecker;
import no.ion.jake.vespa.containerPlugin.Assemble;
import no.ion.jake.vespa.containerPlugin.AssemblerOutput;
import no.ion.jake.vespa.containerPlugin.BundleClassPathMappingsGenerator;
import no.ion.jake.vespa.containerPlugin.OsgiManifestGenerator;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YoleanModule implements JavaModule {

    private final ModuleContext moduleContext;
    private final MavenRepository mavenRepository;
    private final Javac javac;
    private final Jar jar;
    private final Javadoc javadoc;
    private final MavenArtifactId mavenArtifactId;

    private boolean declaredBuildsHasBeenInvoked = false;
    private MavenArtifact mavenArtifact = null;

    public YoleanModule(ModuleContext moduleContext, MavenRepository mavenRepository, Javac javac, Jar jar, Javadoc javadoc) {
        this.moduleContext = moduleContext;
        this.mavenRepository = mavenRepository;
        this.javac = javac;
        this.jar = jar;
        this.javadoc = javadoc;
        this.mavenArtifactId = MavenArtifactId.from("com.yahoo.vespa", moduleName())
                .withVersion("7-SNAPSHOT");
    }

    @Override public String moduleName() { return "yolean"; }
    @Override public MavenArtifactId mavenArtifactId() { return mavenArtifactId; }

    @Override
    public MavenArtifact mavenArtifact() {
        if (!declaredBuildsHasBeenInvoked) {
            throw new IllegalStateException("declareBuilds() has not been called yet");
        }
        return Objects.requireNonNull(mavenArtifact);
    }

    @Override
    public void declareBuilds(Declarator declarator) {
        if (declaredBuildsHasBeenInvoked) {
            throw new IllegalStateException("declareBuilds() cannot be invoked more than once");
        }
        declaredBuildsHasBeenInvoked = true;

        JavaSourceArtifacts sourceArtifacts = new JavaSource("source")
                .addDirectory(Path.of("src/main/java"))
                .declareScan(declarator);

        List<MavenArtifact> providedMavenArtifacts = Stream.of(
                "com.google.guava:guava:20.0",
                "com.yahoo.vespa:annotations:7-SNAPSHOT")
                .map(MavenArtifactId::fromCoordinate)
                .map(mavenArtifact -> mavenRepository.declareDownload(declarator, mavenArtifact))
                .collect(Collectors.toList());

        List<MavenArtifact> testMavenArtifacts = Stream.of(
                "junit:junit:4.12",
                "org.hamcrest:hamcrest-core:1.3") // compile-scope dependency of junit
                .map(MavenArtifactId::fromCoordinate)
                .map(mavenArtifact -> mavenRepository.declareDownload(declarator, mavenArtifact))
                .collect(Collectors.toList());

        Artifact<Path> classesArtifact = new JavaCompiler(javac, "source compilation")
                .addSourceFilesArtifact(sourceArtifacts.javaFilesArtifact())
                .addClassPathEntries(providedMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addJavacArguments(List.of(
                        // Specified in parent POM (except -g?)
                        "-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        "-Xlint:all", "-Xlint:-serial", "-Xlint:-try", "-Xlint:-processing", "-Xlint:-varargs",
                        "-Xlint:-options", "-Werror"))
                .setDestinationDirectory(declarator.moduleContext().pathOf("target/classes"))
                .declareCompile(declarator);

        Artifact<FileSet2> testSourceArtifact = FileTreeScanner.newBuilder(declarator, "test source")
                .includeFiles(moduleContext.pathOf("src/test/java"), true, PathPattern.of("*.java"))
                .declareScan();

        Artifact<Path> testClassesArtifact = new JavaCompiler(javac, "test compilation")
                .addSourceFilesArtifact(testSourceArtifact)
                .addClassPathEntries(providedMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntries(testMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntry(ClassPathEntry.fromExplodedJarArtifact(classesArtifact))
                .addJavacArguments(List.of("-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        "-Xlint:all", "-Xlint:-serial", "-Xlint:-try", "-Xlint:-processing", "-Xlint:-varargs",
                        "-Xlint:-options", "-Werror"))
                .setDestinationDirectory(declarator.moduleContext().pathOf("target/test-classes"))
                .declareCompile(declarator);

        Artifact<Path> bundleClassPathMappingsArtifact = new BundleClassPathMappingsGenerator(moduleContext, this,
                providedMavenArtifacts)
                .declareGenerate(declarator);

        Artifact<Void> testArtifact = new JUnit4TestRunDeclaration("test run")
                .addClassPathEntries(providedMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntries(testMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntry(ClassPathEntry.fromExplodedJarArtifact(classesArtifact))
                .addClassPathEntry(ClassPathEntry.fromExplodedJarArtifact(testClassesArtifact))
                .addClassPathEntry(ClassPathEntry.fromJar(moduleContext.jakeJarPath()))
                .addTestClassesArtifact(testClassesArtifact)
                .declareTestRun(declarator);

        Artifact<Path> manifestArtifact = new OsgiManifestGenerator("osgi manifest", moduleContext, this)
                .addClassPathEntryArtifacts(providedMavenArtifacts)
                .addClassesArtifact(classesArtifact)
                .declareGenerate(declarator);

        AssemblerOutput assemblerOutput = new Assemble(jar, this, classesArtifact, manifestArtifact)
                .declareAssemblies(declarator);

        // TODO: Include pom.xml and pom.properties
        Artifact<Path> sourceJarArtifact = JavaArchiver.forCreatingArchive(jar, "source archive")
                .addDirectoryArtifacts(sourceArtifacts.javaDirectoryArtifacts())
                .setOutputFile(Path.of("target/" + moduleName() + "-sources.jar"))
                .declareArchive(declarator);

        Artifact<Path> javadocDirectoryArtifact = new JavaDocumentation(javadoc, "generate javadoc")
                // TODO: these are identical to JavaCompiler above
                .addClassPathEntries(providedMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                // TODO: add target/generated-sources/annotations ?
                .addSourceDirectoryArtifacts(sourceArtifacts.javaDirectoryArtifacts())
                .addClassesArtifact(classesArtifact)
                .addArguments("-Xdoclint:all,-missing",
                        // javadoc normally prints >=1 line per source file
                        "-quiet")
                .setMavenArtifactId(mavenArtifactId)
                .setHeader("<a href=\"https://docs.vespa.ai\"><img src=\"https://docs.vespa.ai/img/vespa-logo.png\" " +
                        "width=\"100\" height=\"28\" style=\"padding-top:7px\"/></a>")
                .setBottom("Copyright &#169; 2020. All rights reserved.")
                .setDestinationDirectory(Path.of("target/apidocs"))
                .declare(declarator);

        Artifact<Path> javadocJarArtifact = JavaArchiver.forCreatingArchive(jar, "javadoc archive")
                .addDirectoryArtifacts(sourceArtifacts.javaDirectoryArtifacts())
                .setOutputFile(Path.of("target/" + moduleName() + "-javadoc.jar"))
                .declareArchive(declarator);


        Artifact<Void> abiCheckArtifact = AbiChecker.check(assemblerOutput.jarArtifact(), moduleContext, this, declarator);

        var artifactInstaller = new ArtifactInstaller(declarator, mavenRepository)
                .withDependencies(testArtifact, abiCheckArtifact);
        this.mavenArtifact = artifactInstaller.install(assemblerOutput.jarArtifact(), mavenArtifactId);
        MavenArtifact pomMavenArtifact = artifactInstaller.install(Path.of("pom.xml"), mavenArtifactId.withPackaging("pom"));
        MavenArtifact sourceJarMavenArtifact = artifactInstaller.install(sourceJarArtifact, mavenArtifactId.withClassifier("sources"));
        MavenArtifact javadocMavenArtifact = artifactInstaller.install(javadocJarArtifact, mavenArtifactId.withClassifier("javadoc"));
    }
}
