package no.ion.jake.vespa;

import no.ion.jake.BuildContext;
import no.ion.jake.module.ModuleContext;
import no.ion.jake.io.ArtifactInstaller;
import no.ion.jake.java.ClassPathBuilder;
import no.ion.jake.java.Jar;
import no.ion.jake.java.JavaArchiver;
import no.ion.jake.java.JavaCompiler;
import no.ion.jake.java.Javac;
import no.ion.jake.javadoc.JavaDocumentation;
import no.ion.jake.javadoc.Javadoc;
import no.ion.jake.junit4.JUnit4TestRunner;
import no.ion.jake.maven.Scope;
import no.ion.jake.util.Stopwatch;
import no.ion.jake.vespa.abiCheckPlugin.AbiChecker;
import no.ion.jake.vespa.containerPlugin.Assembler;
import no.ion.jake.vespa.containerPlugin.BundleClassPathMappingsGenerator;
import no.ion.jake.vespa.containerPlugin.OsgiManifestGenerator;

import static java.lang.String.format;

public class VespajlibModule {
    private final ModuleContext moduleContext;
    private final ClassPathBuilder classPathBuilder;
    private final JavaCompiler sourceCompiler;
    private final BundleClassPathMappingsGenerator mappingsGenerator;
    private final JUnit4TestRunner testRunner;
    private final JavaCompiler testSourceCompiler;
    private final JavaArchiver sourceJarArtifactArchiver;
    private final JavaDocumentation javaDocumentation;
    private final JavaArchiver javadocArchiver;
    private final OsgiManifestGenerator osgiManifestGenerator;
    private final Assembler assembler;
    private final AbiChecker abiChecker;

    public VespajlibModule(ModuleContext moduleContext, Javac javac, Jar jar, Javadoc javadoc,
                           TestutilModule testutilModule, YoleanModule yoleanModule) {
        this.moduleContext = moduleContext;
        this.classPathBuilder = new ClassPathBuilder(moduleContext)
                .setDefaultScope(Scope.COMPILE)
                .addMavenJar("org.lz4:lz4-java:1.7.1")
                .addMavenJar("org.apache.commons:commons-exec:1.3")
                .addMavenJar("net.java.dev.jna:jna:4.5.2")
                .setDefaultScope(Scope.PROVIDED)
                .addMavenJar("com.google.guava:guava:20.0")
                .addMavenJar("com.yahoo.vespa:annotations:7-SNAPSHOT")
                .addModuleJar(yoleanModule)
                .setDefaultScope(Scope.TEST)
                .addMavenJar("junit:junit:4.12")
                .addMavenJar("org.hamcrest:hamcrest-library:1.3")
                .addMavenJar("org.hamcrest:hamcrest-core:1.3")
                .addMavenJar("org.mockito:mockito-core:3.1.0")
                .addMavenJar("net.bytebuddy:byte-buddy:1.9.10")  // compile scope dep of mockito-core:3.1.0
                .addMavenJar("net.bytebuddy:byte-buddy-agent:1.9.10")  // compile scope dep of mockito-core:3.1.0
                .addMavenJar("org.objenesis:objenesis:2.6")  // compile scope dep of mockito-core:3.1.0
                .addModuleJar(testutilModule)
                .addMavenJar("com.fasterxml.jackson.core:jackson-databind:2.8.11.6")
                .addMavenJar("com.fasterxml.jackson.core:jackson-core:2.8.11")
                .addMavenJar("com.fasterxml.jackson.core:jackson-annotations:2.8.11")
                .setCompileDestinationDirectory("target/classes")
                .setTestCompileDestinationDirectory("target/test-classes");

        this.sourceCompiler = new JavaCompiler(moduleContext, javac)
                .addSourceDirectory("src/main/java")
                .addToClassPath(classPathBuilder.toClassPathForCompile())
                .setDestinationDirectory(classPathBuilder.getCompileDestinationDirectory())
                .addArguments(
                        "-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        // override of compilerArgs:
                        "-Xlint:all", "-Xlint:-try", "-Werror");

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
                        "-O", "-g", "-target", "11", "-source", "11", "-encoding", "UTF-8",
                        // override of compilerArgs:
                        "-Xlint:all", "-Xlint:-try", "-Werror");

        this.sourceJarArtifactArchiver = JavaArchiver.forCreating(moduleContext, jar)
                .setPath("target/" + moduleContext.mavenArtifact().artifactId() + "-sources.jar")
                .includeDirectories(sourceCompiler.getSourceDirectories());

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

        // TODO: There is an updateReleaseInfo set to true for maven-isntall-plugin
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
}
