package no.ion.jake.vespa;

import no.ion.jake.BuildContext;
import no.ion.jake.module.ModuleContext;
import no.ion.jake.io.ArtifactInstaller;
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
import no.ion.jake.util.Stopwatch;
import no.ion.jake.vespa.abiCheckPlugin.AbiChecker;
import no.ion.jake.vespa.containerPlugin.Assembler;
import no.ion.jake.vespa.containerPlugin.BundleClassPathMappingsGenerator;
import no.ion.jake.vespa.containerPlugin.OsgiManifestGenerator;

import static java.lang.String.format;

public class YoleanModule implements JavaModule {
    private final ModuleContext moduleContext;
    private final ClassPathBuilder classPathBuilder;
    private final JavaCompiler sourceCompiler;
    private final JavaCompiler testSourceCompiler;
    private final BundleClassPathMappingsGenerator mappingsGenerator;
    private final JUnit4TestRunner testRunner;
    private final JavaArchiver sourceJarArtifactArchiver;
    private final JavaDocumentation javaDocumentation;
    private final JavaArchiver javadocArchiver;
    private final OsgiManifestGenerator osgiManifestGenerator;
    private final Assembler assembler;
    private final AbiChecker abiChecker;

    public YoleanModule(ModuleContext moduleContext, Javac javac, Javadoc javadoc, Jar jar) {
        this.moduleContext = moduleContext;

        this.classPathBuilder = new ClassPathBuilder(moduleContext)
                .setDefaultScope(Scope.PROVIDED)
                .addMavenJar("com.google.guava:guava:20.0")
                .addMavenJar("com.yahoo.vespa:annotations:7-SNAPSHOT")
                .setDefaultScope(Scope.TEST)
                .addMavenJar("junit:junit:4.12")
                .addMavenJar("org.hamcrest:hamcrest-core:1.3") // compile-scope dependency of junit
                .setCompileDestinationDirectory("target/classes")
                .setTestCompileDestinationDirectory("target/test-classes");

        this.sourceCompiler = new JavaCompiler(moduleContext, javac)
                // TODO: Maven does the following for yolean:
                //    -s .../yolean/target/generated-sources/annotations
                // TODO: source directories:
                // yolean/src/main/java
                // yolean/target/generated-sources/vespa-configgen-plugin
                // yolean/target/generated-sources/annotations
                .addSourceDirectory("src/main/java")
                .addToClassPath(classPathBuilder.toClassPathForCompile())
                .setDestinationDirectory(classPathBuilder.getCompileDestinationDirectory())
                .addArguments(
                        // Specified in parent POM (except -g?)
                        "-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        "-Xlint:all", "-Xlint:-serial", "-Xlint:-try", "-Xlint:-processing", "-Xlint:-varargs",
                        "-Xlint:-options", "-Werror");

        this.mappingsGenerator = new BundleClassPathMappingsGenerator(moduleContext, classPathBuilder);

        this.testRunner = new JUnit4TestRunner()
                .addClassPath(classPathBuilder.toClassPathForTest()
                        .addExplodedJar(moduleContext.project().fileSystem().getPath("/home/hakon/tmp/jake2/classes")))
                .setTestCompileDestinationDirectory(classPathBuilder.getTestCompilationDestinationDirectory());

        this.testSourceCompiler = new JavaCompiler(moduleContext, javac)
                .addSourceDirectory(moduleContext.path().resolve("src/test/java"))
                .setDestinationDirectory(classPathBuilder.getTestCompilationDestinationDirectory())
                .addToClassPath(classPathBuilder.toClassPathForTestCompile())
                .addArguments(
                        // Specified in parent POM (except -g?)
                        "-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        "-Xlint:all", "-Xlint:-serial", "-Xlint:-try", "-Xlint:-processing", "-Xlint:-varargs",
                        "-Xlint:-options", "-Werror");

        // TODO: Include pom.xml and pom.properties
        this.sourceJarArtifactArchiver = JavaArchiver.forCreating(moduleContext, jar)
                .setPath("target/" + moduleContext.mavenArtifact().artifactId() + "-sources.jar")
                .includeDirectories(sourceCompiler.getSourceDirectories());

        // Verified give the same target/apidocs as with mvn
        this.javaDocumentation = new JavaDocumentation(moduleContext, javadoc)
                .addToClassPath(sourceCompiler.getClassPath())
                .addSourceDirectories(sourceCompiler.getSourceDirectories())
                .setDestinationDirectory("target/apidocs")
                .addArguments("-Xdoclint:all,-missing")
                // javadoc normally prints >=1 line per source file
                .addArguments("-quiet")
                .setHeader("<a href=\"https://docs.vespa.ai\"><img src=\"https://docs.vespa.ai/img/vespa-logo.png\" " +
                        "width=\"100\" height=\"28\" style=\"padding-top:7px\"/></a>")
                .setBottom("Copyright &#169; 2020. All rights reserved.");

        this.javadocArchiver = JavaArchiver.forCreating(moduleContext, jar)
                .setPath("target/" + moduleContext.mavenArtifact().artifactId() + "-javadoc.jar")
                .includeDirectories("target/apidocs");

        this.osgiManifestGenerator = new OsgiManifestGenerator(moduleContext)
                .setClassPathBuilder(classPathBuilder);


        this.assembler = new Assembler(jar, classPathBuilder);

        this.abiChecker = new AbiChecker();
    }

    public void build(BuildContext buildContext) {
        buildContext.run(sourceCompiler);
        buildContext.run(testSourceCompiler);
        buildContext.run(mappingsGenerator);
        buildContext.run(testRunner);
        buildContext.run(osgiManifestGenerator);
        buildContext.run(javaDocumentation);
        buildContext.run(sourceJarArtifactArchiver);
        buildContext.run(javadocArchiver);

        assembler.build(buildContext);

        abiChecker.setJarPath(assembler.getJarPath());
        buildContext.run(abiChecker);

        ArtifactInstaller installer = new ArtifactInstaller(buildContext);
        installer.install(assembler.getJarPath(), moduleContext.mavenArtifact());
        installer.install("pom.xml", moduleContext.mavenArtifact().withPackaging("pom"));
        installer.install(sourceJarArtifactArchiver.path(), moduleContext.mavenArtifact().withClassifier("sources"));
        installer.install(javadocArchiver.path(), moduleContext.mavenArtifact().withClassifier("javadoc"));
    }

    @Override
    public MavenArtifact jarMavenArtifact() {
        return moduleContext.mavenArtifact();
    }
}
