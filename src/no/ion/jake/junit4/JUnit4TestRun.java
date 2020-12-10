package no.ion.jake.junit4;

import no.ion.jake.AbortException;
import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.container.Container;
import no.ion.jake.java.ClassPath;
import no.ion.jake.java.ClassPathEntry;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class JUnit4TestRun implements Build {
    private static final Set<String> PACKAGES_ACCESSIBLE_TO_UNIT_TESTS = Set.of(
            "no.ion.jake", "no.ion.jake.junit4", "no.ion.jake.util",
            // TODO: Remove this once BuildContext has been moved to one of the above
            "no.ion.jake.build");

    private final String name;
    private final Artifact<Path> testClassesArtifact;
    private final List<ClassPathEntry> classPathEntries;
    private final Artifact<Void> testArtifact;

    public JUnit4TestRun(String name, Artifact<Path> testClassesArtifact, List<ClassPathEntry> classPathEntries, Artifact<Void> testArtifact) {
        this.name = name;
        this.testClassesArtifact = testClassesArtifact;
        this.classPathEntries = classPathEntries;
        this.testArtifact = testArtifact;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void build(BuildContext buildContext) {
        Path testClassesPath = testClassesArtifact.detail();

        List<String> testClassNames = new ArrayList<>();
        uncheckIO(() -> Files.walkFileTree(testClassesPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = testClassesPath.relativize(file).normalize().toString();
                if (!relativePath.endsWith(".class")) {
                    return FileVisitResult.CONTINUE;
                }
                String className = relativePath
                        .substring(0, relativePath.length() - ".class".length())
                        .replace('/', '.');
                if (className.indexOf('$') == -1) {
                    testClassNames.add(className);
                } // TODO: include inner classes(?)

                return FileVisitResult.CONTINUE;
            }
        }));

        Predicate<String> useOurClassLoader = className -> {
            if (className.startsWith("no.ion.jake.")) {
                // Everything in or below container is by definition loaded by the container class loader
                if (className.startsWith("no.ion.jake.junit4.container.")) {
                    return false;
                }

                // Whitelist of packages visible to the container
                String packageName = className.substring(0, className.lastIndexOf('.'));
                return PACKAGES_ACCESSIBLE_TO_UNIT_TESTS.contains(packageName);
            }

            return false;
        };

        ClassPath classPath = new ClassPath();
        classPathEntries.forEach(entry -> {
            switch (entry.getType()) {
                case EXPLODED_JAR:
                    classPath.addExplodedJar(entry.getValidatedPath().toAbsolutePath());
                    return;
                case JAR:
                    classPath.addJar(entry.getValidatedPath().toAbsolutePath());
                    return;
            }
            throw new IllegalArgumentException("unknown class path entry type: " + entry.getType());
        });

        Container junit4Container = Container.create("junit4", classPath, useOurClassLoader);
        JUnit4TestResults results = junit4Container.invoke(
                JUnit4TestResults.class,
                "no.ion.jake.junit4.container.Runner",
                "run",
                new Class<?>[] { BuildContext.class, List.class },
                buildContext,
                testClassNames);

        if (!results.success()) {
            // TODO: Handle this correctly with concurrent execution
            throw new AbortException(results.message());
        }

        buildContext.newPublicationOf(testArtifact)
                .log(results.message())
                .publish(null);
    }
}
