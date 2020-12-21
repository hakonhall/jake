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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestutilModule implements JavaModule {
    private final ModuleContext moduleContext;
    private final MavenRepository mavenRepository;
    private final Javac javac;
    private final Jar jar;
    private final Javadoc javadoc;
    private final String name;
    private final MavenArtifactId mavenArtifactId;

    private boolean declaredBuildsHasBeenInvoked = false;
    private MavenArtifact mavenArtifact = null;

    public TestutilModule(ModuleContext moduleContext, MavenRepository mavenRepository, Javac javac, Jar jar, Javadoc javadoc) {
        this.moduleContext = moduleContext;
        this.mavenRepository = mavenRepository;
        this.javac = javac;
        this.jar = jar;
        this.javadoc = javadoc;
        this.name = "testutil";
        this.mavenArtifactId = MavenArtifactId.from("com.yahoo.vespa", name)
                .withVersion("7-SNAPSHOT");
    }

    @Override public String moduleName() { return name; }
    @Override public MavenArtifactId mavenArtifactId() { return mavenArtifactId; }

    @Override public MavenArtifact mavenArtifact() {
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

        MavenArtifactId mavenArtifactId = MavenArtifactId.from("com.yahoo.vespa", name)
                .withVersion("7-SNAPSHOT");

        JavaSourceArtifacts sourceArtifacts = new JavaSource("source")
                .addDirectory(Path.of("src/main/java"))
                .declareScan(declarator);

        List<MavenArtifact> providedMavenArtifacts = Stream.of(
                "com.google.guava:guava:20.0",
                "com.google.inject:guice:3.0:no_aop")
                .map(MavenArtifactId::fromCoordinate)
                .map(mavenArtifact -> mavenRepository.declareDownload(declarator, mavenArtifact))
                .collect(Collectors.toList());

        List<MavenArtifact> compileMavenArtifacts = Stream.of(
                "org.hamcrest:hamcrest-core:1.3",
                "org.hamcrest:hamcrest-library:1.3",
                "uk.co.datumedge:hamcrest-json:0.2",
                "junit:junit:4.12",
                "com.google.jimfs:jimfs:1.1",
                // jackson-databind also depends on jackson-annotation, but compilation works without it
                "com.fasterxml.jackson.core:jackson-databind:2.8.11.6",
                "com.fasterxml.jackson.core:jackson-core:2.8.11")
                .map(MavenArtifactId::fromCoordinate)
                .map(mavenArtifact -> mavenRepository.declareDownload(declarator, mavenArtifact))
                .collect(Collectors.toList());

        Artifact<Path> classesArtifact = new JavaCompiler(javac, null)
                .addSourceFilesArtifact(sourceArtifacts.javaFilesArtifact())
                .addClassPathEntries(providedMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntries(compileMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addJavacArguments(List.of(
                        // Inherited from parent POM (except -g?)
                        "-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        // These are specified in testutil/pom.xml, overriding parent/pom.xml.
                        "-Xlint:all", "-Xlint:-varargs", "-Werror"))
                .setDestinationDirectory(declarator.moduleContext().pathOf("target/classes"))
                .declareCompile(declarator);

        Artifact<FileSet2> testSourceArtifact = FileTreeScanner.newBuilder(declarator, "test source")
                .includeFiles(moduleContext.pathOf("src/test/java"), true, PathPattern.of("*.java"))
                .declareScan();

        Artifact<Path> testClassesArtifact = new JavaCompiler(javac, "test")
                .addSourceFilesArtifact(testSourceArtifact)
                .addClassPathEntries(providedMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntries(compileMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntry(ClassPathEntry.fromExplodedJarArtifact(classesArtifact))
                .addJavacArguments(List.of("-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        "-Xlint:all", "-Xlint:-varargs", "-Werror"))
                .setDestinationDirectory(declarator.moduleContext().pathOf("target/test-classes"))
                .declareCompile(declarator);

        Artifact<Void> testArtifact = new JUnit4TestRunDeclaration("test run")
                .addClassPathEntries(providedMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntries(compileMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntry(ClassPathEntry.fromExplodedJarArtifact(classesArtifact))
                .addClassPathEntry(ClassPathEntry.fromExplodedJarArtifact(testClassesArtifact))
                .addClassPathEntry(ClassPathEntry.fromJar(moduleContext.jakeJarPath()))
                .addTestClassesArtifact(testClassesArtifact)
                .declareTestRun(declarator);

        Artifact<Path> jarArtifact = JavaArchiver.forCreatingArchive(jar, "java archive")
                .addDirectoryArtifact(classesArtifact)
                .setOutputFile(declarator.moduleContext().pathOf("target/testutil.jar"))
                .declareArchive(declarator);

        // TODO: Include pom.xml and pom.properties
        Artifact<Path> sourceJarArtifact = JavaArchiver.forCreatingArchive(jar, "source archive")
                .addDirectoryArtifacts(sourceArtifacts.javaDirectoryArtifacts())
                .setOutputFile(Path.of("target/testutil-sources.jar"))
                .declareArchive(declarator);

        Artifact<Path> javadocDirectoryArtifact = new JavaDocumentation(javadoc, "generate javadoc")
                // TODO: these are identical to JavaCompiler above
                .addClassPathEntries(providedMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
                .addClassPathEntries(compileMavenArtifacts.stream().map(ClassPathEntry::fromMavenArtifact).collect(Collectors.toList()))
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
                .setOutputFile(Path.of("target/testutil-javadoc.jar"))
                .declareArchive(declarator);

        var artifactInstaller = new ArtifactInstaller(declarator, mavenRepository)
                .withDependencies(testArtifact);
        this.mavenArtifact = artifactInstaller.install(jarArtifact, mavenArtifactId);
        MavenArtifact pomMavenArtifact = artifactInstaller.install(Path.of("pom.xml"), mavenArtifactId.withPackaging("pom"));
        MavenArtifact sourceJarMavenArtifact = artifactInstaller.install(sourceJarArtifact, mavenArtifactId.withClassifier("sources"));
        MavenArtifact javadocMavenArtifact = artifactInstaller.install(javadocJarArtifact, mavenArtifactId.withClassifier("javadoc"));
    }
}
