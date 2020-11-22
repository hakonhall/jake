package no.ion.jake.junit4;

import no.ion.jake.AbortException;
import no.ion.jake.BuildContext;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildResult;
import no.ion.jake.container.Container;
import no.ion.jake.java.ClassPath;

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

public class JUnit4TestRunner implements Build {
    private static final Set<String> PACKAGES_ACCESSIBLE_TO_UNIT_TESTS = Set.of(
            "no.ion.jake", "no.ion.jake.junit4", "no.ion.jake.util");

    private final ClassPath classPath = new ClassPath();
    private Path testCompilationDestinationDirectory = null;

    public JUnit4TestRunner() {}

    public JUnit4TestRunner addClassPath(ClassPath classPath) {
        this.classPath.addFrom(classPath);
        return this;
    }

    public JUnit4TestRunner setTestCompileDestinationDirectory(Path testCompilationDestinationDirectory) {
        this.testCompilationDestinationDirectory = testCompilationDestinationDirectory;
        return this;
    }

    @Override
    public BuildResult build(BuildContext buildContext) {
        if (testCompilationDestinationDirectory == null) {
            throw new IllegalStateException("testCompilationDestinationDirectory has not been set");
        }
        Path testCompilationDestinationPath = buildContext.moduleContext().path().resolve(testCompilationDestinationDirectory);

        List<String> testClassNames = new ArrayList<>();
        uncheckIO(() -> Files.walkFileTree(testCompilationDestinationPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = testCompilationDestinationPath.relativize(file).normalize().toString();
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

        Container junit4Container = Container.create("junit4", classPath, useOurClassLoader);
        JUnit4TestResults results = junit4Container.invoke(
                JUnit4TestResults.class,
                "no.ion.jake.junit4.container.Runner",
                "run",
                new Class<?>[] { BuildContext.class, List.class },
                buildContext,
                testClassNames);

        if (!results.success()) {
            throw new AbortException(results.message());
        }

        return BuildResult.of(false, results.message(), false);
    }
}
