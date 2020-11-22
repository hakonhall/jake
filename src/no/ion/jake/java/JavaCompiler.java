package no.ion.jake.java;

import no.ion.jake.BuildContext;
import no.ion.jake.build.Build;
import no.ion.jake.module.ModuleContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import static no.ion.jake.util.Exceptions.uncheckIO;

/**
 * A thread-UNSAFE builder for one or more sequential Java compilations.
 */
public class JavaCompiler implements Build {
    private final Javac javac;
    private final FileSystem fileSystem;
    private final Path modulePath;
    private final List<Path> sourceDirectories = new ArrayList<>();
    private Path destinationDirectory = null;
    private final ClassPath classPath = new ClassPath();
    private final ArrayList<String> passthroughJavacArguments = new ArrayList<>();

    public JavaCompiler(ModuleContext moduleContext, Javac javac) {
        this.fileSystem = moduleContext.project().fileSystem();
        this.modulePath = moduleContext.path();
        this.javac = javac;
    }

    public JavaCompiler addSourceDirectory(String path) {
        return addSourceDirectory(fileSystem.getPath(path));
    }

    public JavaCompiler addSourceDirectory(Path path) {
        if (!path.isAbsolute()) {
            path = modulePath.resolve(path);
        }

        if (!Files.exists(path)) {
            throw new UncheckedIOException(new NoSuchFileException(path.toString()));
        }

        sourceDirectories.add(path);
        return this;
    }

    public List<Path> getSourceDirectories() { return List.copyOf(sourceDirectories); }

    public Path getDestinationDirectory() {
        return Objects.requireNonNull(destinationDirectory, "Destination directory not set");
    }

    public JavaCompiler setDestinationDirectory(String path) {
        return setDestinationDirectory(modulePath.getFileSystem().getPath(path));
    }

    public JavaCompiler setDestinationDirectory(Path path) {
        this.destinationDirectory = Objects.requireNonNull(path);
        return this;
    }

    public JavaCompiler addToClassPath(ClassPath classPath) {
        this.classPath.addFrom(classPath);
        return this;
    }

    public ClassPath getClassPath() {
        return new ClassPath(classPath);
    }

    /** Adds arguments that will be passed through to the javac compiler as-is. */
    public JavaCompiler addArguments(String... arguments) {
        passthroughJavacArguments.ensureCapacity(passthroughJavacArguments.size() + arguments.length);
        for (var argument : arguments) {
            passthroughJavacArguments.add(argument);
        }

        return this;
    }

    @Override
    public JavaCompilationResult build(BuildContext context) throws JavaCompilationException {

        var arguments = new ArrayList<String>(passthroughJavacArguments);

        classPath.validateExistenceOfPaths();
        arguments.add("-cp");
        arguments.add(classPath.toString());

        if (destinationDirectory == null) {
            throw new JavaCompilationException("destination directory not set");
        }
        Path destinationPath = destinationDirectory.isAbsolute() ?
                destinationDirectory :
                modulePath.resolve(destinationDirectory);
        arguments.add("-d");
        arguments.add(destinationPath.toString());

        uncheckIO(() -> Files.createDirectories(destinationPath));

        List<String> sourceFiles = findSourceFiles();
        if (sourceFiles.isEmpty()) {
            return new JavaCompilationResult(0, null, destinationDirectory);
        }
        arguments.addAll(sourceFiles);

        context.logDebug(() -> "javac " + String.join(" ", arguments));

        Javac.CompileResult result = javac.compile(arguments);

        if (result.code != 0) {
            throw new JavaCompilationException(result.message);
        }

        var result2 = new JavaCompilationResult(sourceFiles.size(), result.message, destinationDirectory);

        result2.warning().ifPresent(context::logWarning);
        return result2;
    }

    private List<String> findSourceFiles() {
        List<String> javacSourceFiles = new ArrayList<>();

        Deque<Path> directoryQueue = new ArrayDeque<>(sourceDirectories);

        while (!directoryQueue.isEmpty()) {
            Path directory = directoryQueue.removeFirst();
            try (DirectoryStream<Path> dentryPathStream = Files.newDirectoryStream(directory)) {
                for (Path path : dentryPathStream) {
                    if (Files.isDirectory(path)) {
                        directoryQueue.addLast(path);
                    } else if (path.getFileName().toString().endsWith(".java")) {
                        javacSourceFiles.add(path.toString());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return javacSourceFiles;
    }
}
