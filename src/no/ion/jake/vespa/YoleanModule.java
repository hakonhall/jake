package no.ion.jake.vespa;

import no.ion.jake.BuildContext;
import no.ion.jake.ModuleContext;
import no.ion.jake.io.ArtifactInstaller;
import no.ion.jake.java.ClassPathBuilder;
import no.ion.jake.java.Jar;
import no.ion.jake.java.JavaArchiver;
import no.ion.jake.java.JavaArchiverResult;
import no.ion.jake.java.JavaCompilationResult;
import no.ion.jake.java.JavaCompiler;
import no.ion.jake.java.JavaModule;
import no.ion.jake.java.Javac;
import no.ion.jake.javadoc.JavaDocumentation;
import no.ion.jake.javadoc.JavaDocumentationResult;
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

        this.osgiManifestGenerator = new OsgiManifestGenerator(moduleContext);

        this.assembler = new Assembler(jar, classPathBuilder);

        this.abiChecker = new AbiChecker();
    }

    public void build(BuildContext buildContext) {
        compile(sourceCompiler, buildContext, "source");
        compile(testSourceCompiler, buildContext, "test source");

        timedRun(() -> mappingsGenerator.build(buildContext), buildContext,
                "wrote " + mappingsGenerator.outputPath());

        testRunner.build(buildContext);

        timedRun(() -> osgiManifestGenerator.build(buildContext, classPathBuilder), buildContext, "wrote target/classes/META-INF/MANIFEST.MF");

        JavaDocumentationResult javadocResult = javaDocumentation.build(buildContext);
        if (!javadocResult.warning().isEmpty()) {
            buildContext.logWarning(javadocResult.warning());
        }
        buildContext.logInfo(format("wrote javadoc to %s in %.3f s",
                javaDocumentation.getDestinationDirectory(),
                javadocResult.getSeconds()));

        archive(buildContext, sourceJarArtifactArchiver);
        archive(buildContext, javadocArchiver);

        assembler.build(buildContext);

        timedRun(() -> abiChecker.build(buildContext, assembler.getJarPath()), buildContext, "ran abi-check");

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

    private static void timedRun(Runnable runnable, BuildContext context, String description) {
        var runningStopwatch = Stopwatch.start();
        runnable.run();
        double manifestGenerationSeconds = runningStopwatch.stop().toMillis() / 1000.0;
        context.logInfo(format("%s in %.3f s", description, manifestGenerationSeconds));
    }

    private static void compile(JavaCompiler compilation, BuildContext context, String name) {
        JavaCompilationResult result = compilation.compile(context);
        result.warning().ifPresent(context::logWarning);
        if (result.numFilesCompiled() > 0) {
            context.logInfo(format("compiled %d %s files to %s in %.3f s",
                    result.numFilesCompiled(),
                    name,
                    compilation.getDestinationDirectory(),
                    result.getSeconds()));
        }
    }

    private static void archive(BuildContext buildContext, JavaArchiver javaArchiver) {
        JavaArchiverResult archivingResult = javaArchiver.archive(buildContext);
        archivingResult.warning().ifPresent(buildContext::logWarning);
        buildContext.logInfoFormat("built %s in %.3f s", javaArchiver.path().toString(), archivingResult.getSeconds());
    }
}
