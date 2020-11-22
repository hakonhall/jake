package no.ion.jake.java;

import no.ion.jake.BuildContext;
import no.ion.jake.build.Build;
import no.ion.jake.module.ModuleContext;
import no.ion.jake.util.Java;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class JavaArchiver implements Build {
    private final ModuleContext moduleContext;
    private final Jar jar;
    private final String mode;
    private final List<Path> directories = new ArrayList<>();

    private Path path = null;
    private String mainClass = null;
    private Path manifestPath = null;

    public static JavaArchiver forCreating(ModuleContext moduleContext, Jar jar) {
        return new JavaArchiver(moduleContext, jar, "-c");
    }

    private JavaArchiver(ModuleContext moduleContext, Jar jar, String mode) {
        this.moduleContext = moduleContext;
        this.jar = jar;
        this.mode = mode;
    }

    public JavaArchiver setPath(String outputPath) {
        return setPath(moduleContext.project().fileSystem().getPath(outputPath));
    }

    /**
     * The path of the JAR file, possibly relative the module directory.
     * Parent directories will be created if they do not exist (in {@link #build(BuildContext)}).
     */
    public JavaArchiver setPath(Path outputPath) {
        this.path = outputPath;
        return this;
    }

    public Path path() { return path; }

    public JavaArchiver setMainClass(String className) {
        if (!Java.isValidClassName(className)) {
            throw new IllegalArgumentException("not a valid class name: " + className);
        }
        this.mainClass = className;
        return this;
    }

    public JavaArchiver setManifestPath(Path manifestPath) {
        this.manifestPath = manifestPath;
        return this;
    }

    public JavaArchiver includeDirectory(Path directory) {
        this.directories.add(directory);
        return this;
    }

    public JavaArchiver includeDirectories(String... directories) {
        Stream.of(directories).map(moduleContext.project().fileSystem()::getPath).forEach(this.directories::add);
        return this;
    }

    public JavaArchiver includeDirectories(List<Path> directories) {
        directories.forEach(this::includeDirectory);
        return this;
    }

    @Override
    public JavaArchiverResult build(BuildContext context) {
        var jarArguments = new ArrayList<String>();
        jarArguments.add(mode);

        Objects.requireNonNull(path);
        Path jarPath = path.isAbsolute() ? path : context.moduleContext().path().resolve(path);
        uncheckIO(() -> Files.createDirectories(jarPath.getParent()));
        jarArguments.add("-f");
        jarArguments.add(jarPath.toString());

        if (mainClass != null) {
            jarArguments.add("-e");
            jarArguments.add(mainClass);
        }

        if (manifestPath != null) {
            Path absoluteManifestPath = manifestPath.isAbsolute() ? manifestPath : moduleContext.path().resolve(manifestPath);
            if (!Files.isRegularFile(absoluteManifestPath)) {
                throw new UncheckedIOException(new NoSuchFileException(absoluteManifestPath.toString()));
            }
            jarArguments.add("-m");
            jarArguments.add(absoluteManifestPath.toString());
        }

        directories.forEach(directory -> {
            Path path = directory.isAbsolute() ? directory : moduleContext.path().resolve(directory);
            if (!Files.isDirectory(path)) {
                throw new UncheckedIOException(new NoSuchFileException("no such directory: " + path));
            }

            jarArguments.add("-C");
            jarArguments.add(path.toString());
            jarArguments.add(".");
        });

        context.logDebug(() -> "jar " + String.join(" ", jarArguments));

        Jar.Result result = jar.run(jarArguments);

        if (result.code() == 0) {
            String warning = result.out();
            if (!warning.isEmpty()) {
                context.logWarning(warning);
            }
            return new JavaArchiverResult(result.out(), path);
        } else {
            throw new JavaArchiverException(result.out());
        }
    }
}
