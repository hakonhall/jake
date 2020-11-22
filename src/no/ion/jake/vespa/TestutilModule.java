package no.ion.jake.vespa;

import no.ion.jake.BuildContext;
import no.ion.jake.module.ModuleContext;
import no.ion.jake.io.ArtifactInstaller;
import no.ion.jake.io.FileSet;
import no.ion.jake.io.PathPattern;
import no.ion.jake.java.ClassPathBuilder;
import no.ion.jake.java.Jar;
import no.ion.jake.java.JavaArchiver;
import no.ion.jake.java.JavaCompiler;
import no.ion.jake.java.JavaModule;
import no.ion.jake.java.Javac;
import no.ion.jake.javadoc.JavaDocumentation;
import no.ion.jake.javadoc.Javadoc;
import no.ion.jake.junit4.JUnit4TestRunner;
import no.ion.jake.maven.MavenArtifact;
import no.ion.jake.maven.Scope;

/**
 * With a missing testutil/target:
 *     time java -cp jar/no.ion.jake-0.0.1.jar no.ion.jake.vespa.Main
 *         real	0m1,160s
 *         user	0m6,761s
 *         sys	0m0,298s
 *
 *     time mvn -nsu install in testutil
 *         real	0m4,594s
 *         user	0m21,765s
 *         sys	0m0,683s
 */
public class TestutilModule implements JavaModule {
    private final ModuleContext moduleContext;
    private final ClassPathBuilder classPathBuilder;
    private final JavaCompiler sourceCompilation;
    private final JavaCompiler testCompilation;
    private final JUnit4TestRunner testRunner;
    private final JavaArchiver jarArtifactArchiver;
    private final JavaArchiver sourceJarArtifactArchiver;
    private final JavaDocumentation javaDocumentation;
    private final JavaArchiver javadocArchiver;

    public TestutilModule(ModuleContext moduleContext, Javac javac, Javadoc javadoc, Jar jar) {
        this.moduleContext = moduleContext;

        this.classPathBuilder = new ClassPathBuilder(moduleContext)
                .setDefaultScope(Scope.PROVIDED)
                .addMavenJar("com.google.guava:guava:20.0")
                .addMavenJar("com.google.inject:guice:3.0:no_aop")

                .setDefaultScope(Scope.COMPILE)
                .addMavenJar("org.hamcrest:hamcrest-core:1.3")
                .addMavenJar("org.hamcrest:hamcrest-library:1.3")
                .addMavenJar("uk.co.datumedge:hamcrest-json:0.2")
                .addMavenJar("junit:junit:4.12")
                .addMavenJar("com.google.jimfs:jimfs:1.1")
                .addMavenJar("com.fasterxml.jackson.core:jackson-databind:2.8.11.6")

                // jackson-databind depends on these two, but annotations is not necessary for successful compilation.
                .addMavenJar("com.fasterxml.jackson.core:jackson-core:2.8.11")
                //.addMavenJar("com.fasterxml.jackson.core:jackson-annotations:2.8.11")

                .setCompileDestinationDirectory("target/classes")
                .setTestCompileDestinationDirectory("target/test-classes");

        FileSet sourceFiles = new FileSet(moduleContext.path())
                .includeFiles("src/main/java", PathPattern.of("*.java"));

        FileSet resourcesFiles = new FileSet(moduleContext.path())
                .includeFiles("src/main/resources");

        FileSet explodedJar = new FileSet(moduleContext.path())
                .includeFiles("target/classes");

        FileSet testSourceFiles = new FileSet(moduleContext.path())
                .includeFiles("src/test/java", PathPattern.of("*.java"));

        FileSet testResourcesFiles = new FileSet(moduleContext.path())
                .includeFiles("src/main/resources");

        FileSet explodedTestJar = new FileSet(moduleContext.path())
                .includeFiles("target/test-classes");

        this.sourceCompilation = new JavaCompiler(moduleContext, javac)
                .addSourceDirectory(moduleContext.path().resolve("src/main/java"))
                .setDestinationDirectory(classPathBuilder.getCompileDestinationDirectory())
                .addToClassPath(classPathBuilder.toClassPathForCompile())
                .addArguments(
                        // Inherited from parent POM (except -g?)
                        "-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        // These are specified in testutil/pom.xml, overriding parent/pom.xml.
                        "-Xlint:all", "-Xlint:-varargs", "-Werror");

        this.testCompilation = new JavaCompiler(moduleContext, javac)
                .addSourceDirectory(moduleContext.path().resolve("src/test/java"))
                .setDestinationDirectory(classPathBuilder.getTestCompilationDestinationDirectory())
                .addToClassPath(classPathBuilder.toClassPathForTestCompile())
                .addArguments("-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        "-Xlint:all", "-Xlint:-varargs", "-Werror");

        this.testRunner = new JUnit4TestRunner()
                .addClassPath(classPathBuilder.toClassPathForTest()
                        .addExplodedJar(moduleContext.project().fileSystem().getPath("/home/hakon/tmp/jake2/classes")))
                .setTestCompileDestinationDirectory(classPathBuilder.getTestCompilationDestinationDirectory());

        this.jarArtifactArchiver = JavaArchiver.forCreating(moduleContext, jar)
                .setPath("target/testutil.jar")
                .includeDirectory(classPathBuilder.getCompileDestinationDirectory());

        // TODO: Include pom.xml and pom.properties
        this.sourceJarArtifactArchiver = JavaArchiver.forCreating(moduleContext, jar)
                .setPath("target/testutil-sources.jar")
                .includeDirectories(sourceCompilation.getSourceDirectories());

        this.javaDocumentation = new JavaDocumentation(moduleContext, javadoc)
                .addToClassPath(sourceCompilation.getClassPath())
                .addSourceDirectories(sourceCompilation.getSourceDirectories())
                .setDestinationDirectory("target/apidocs")
                .addArguments("-Xdoclint:all,-missing")
                // javadoc normally prints >=1 line per source file
                .addArguments("-quiet")
                .setHeader("<a href=\"https://docs.vespa.ai\"><img src=\"https://docs.vespa.ai/img/vespa-logo.png\" " +
                        "width=\"100\" height=\"28\" style=\"padding-top:7px\"/></a>")
                .setBottom("Copyright &#169; 2020. All rights reserved.");

        this.javadocArchiver = JavaArchiver.forCreating(moduleContext, jar)
                .setPath("target/testutil-javadoc.jar")
                .includeDirectories("target/apidocs");
    }

    public void build(BuildContext buildContext) {
        buildContext.run(sourceCompilation);
        buildContext.run(testCompilation);
        buildContext.run(testRunner);
        buildContext.run(jarArtifactArchiver);
        buildContext.run(sourceJarArtifactArchiver);
        buildContext.run(javaDocumentation);
        buildContext.run(javadocArchiver);

        ArtifactInstaller installer = new ArtifactInstaller(buildContext);
        installer.install(jarArtifactArchiver.path(), moduleContext.mavenArtifact());
        installer.install("pom.xml", moduleContext.mavenArtifact().withPackaging("pom"));
        installer.install(sourceJarArtifactArchiver.path(), moduleContext.mavenArtifact().withClassifier("sources"));
        installer.install(javadocArchiver.path(), moduleContext.mavenArtifact().withClassifier("javadoc"));
    }

    @Override
    public MavenArtifact jarMavenArtifact() {
        return moduleContext.mavenArtifact();
    }
}
