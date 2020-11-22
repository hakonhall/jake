package no.ion.jake.javadoc;

import no.ion.jake.BuildContext;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildResult;
import no.ion.jake.module.ModuleContext;
import no.ion.jake.io.FileSet;
import no.ion.jake.io.PathPattern;
import no.ion.jake.java.ClassPath;
import no.ion.jake.util.Java;
import no.ion.jake.util.Stopwatch;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class JavaDocumentation implements Build {
    private final ModuleContext module;
    private final Javadoc javadoc;
    private final ClassPath classPath = new ClassPath();
    private final List<Path> sourceDirectories = new ArrayList<>();
    private final List<String> extraArguments = new ArrayList<>();

    private Path destinationDirectory = null;
    private String header = null;
    private String bottom = null;

    public JavaDocumentation(ModuleContext module, Javadoc javadoc) {
        this.module = module;
        this.javadoc = javadoc;
    }

    public JavaDocumentation addToClassPath(ClassPath classPath) {
        this.classPath.addFrom(classPath);
        return this;
    }

    public JavaDocumentation addSourceDirectories(List<Path> directories) {
        // TODO: add target/generated-sources/annotations ?
        this.sourceDirectories.addAll(directories);
        return this;
    }

    public Path getDestinationDirectory() {
        return Objects.requireNonNull(destinationDirectory, "Destination directory not set");
    }

    public JavaDocumentation setDestinationDirectory(String directory) {
        return setDestinationDirectory(module.project().fileSystem().getPath(directory));
    }

    public JavaDocumentation setDestinationDirectory(Path directory) {
        this.destinationDirectory = directory;
        return this;
    }

    public JavaDocumentation setHeader(String header) {
        this.header = header;
        return this;
    }

    public JavaDocumentation setBottom(String bottom) {
        this.bottom = bottom;
        return this;
    }

    public JavaDocumentation addArguments(String... arguments) {
        extraArguments.addAll(Arrays.asList(arguments));
        return this;
    }

    public BuildResult build(BuildContext buildContext) {
        var arguments = new ArrayList<String>();

        arguments.add("-cp");
        arguments.add(classPath.toString());

        if (sourceDirectories.isEmpty()) {
            throw new JavaDocumentationException("no source directories have been specified");
        }
        String sourcePath = sourceDirectories.stream()
                .map(directory -> {
                    if (!Files.isDirectory(directory)) {
                        throw new UncheckedIOException(new NotDirectoryException(directory.toString()));
                    }
                    return directory.toString();
                })
                .collect(Collectors.joining(":"));
        arguments.add("--source-path");
        arguments.add(sourcePath);

        if (destinationDirectory == null) {
            throw new JavaDocumentationException("destination directory not set");
        }
        Path destinationPath = destinationDirectory.isAbsolute() ?
                destinationDirectory :
                module.path().resolve(destinationDirectory);
        uncheckIO(() -> Files.createDirectories(destinationPath));
        arguments.add("-d");
        arguments.add(destinationPath.toString());

        if (header != null) {
            arguments.add("-header");
            arguments.add(header);
        }

        if (bottom != null) {
            arguments.add("-bottom");
            arguments.add(bottom);
        }

        String title = module.name() + " " + module.mavenArtifact().version() + " API";
        arguments.add("-doctitle");
        arguments.add(title);
        arguments.add("-windowtitle");
        arguments.add(title);

        arguments.add("-encoding");
        arguments.add("UTF-8");
        arguments.add("-charset");
        arguments.add("UTF-8");
        arguments.add("-docencoding");
        arguments.add("UTF-8");

        //arguments.add("-quiet");
        arguments.add("-author");
        arguments.add("-use");
        arguments.add("-version");

        arguments.addAll(extraArguments);

        // TODO: This is interesting because these packages are derived from all source files, which requires
        // the generation of the config sources, i.e. not available in the constructor.
        // javaDocumentation.addPackages("com.yahoo.test.json", "com.yahoo.test", "com.yahoo.vespa.test.file");
        List<String> packages = findPackages();
        arguments.addAll(packages);

        buildContext.logDebug(() -> "javadoc " + String.join(" ", arguments));

        var runningStopwatch = Stopwatch.start();
        Javadoc.Result result = javadoc.run(arguments);
        Duration duration = runningStopwatch.stop();

        if (result.code() == 0) {
            String warning = result.out();
            if (!warning.isEmpty()) {
                buildContext.logWarning(warning);
            }
            return BuildResult.of("wrote javadoc to " + destinationDirectory);
        } else {
            throw new JavaDocumentationException(result.out());
        }
    }

    /**
     * Returns the packages defined in the source directory, in sorted order.
     *
     * <p>Warning: Calculated based on which source directories have *.java files, which is a heuristic and strictly
     * speaking incorrect as the package of a source file can be unrelated to its path.</p>
     */
    private List<String> findPackages() {
        return sourceDirectories.stream()
                .flatMap(sourceDirectory ->
                    new FileSet()
                            .includeFiles(sourceDirectory, PathPattern.of("*.java"))
                            .scan()
                            .toFileInfoList()
                            .stream()
                            .map(fileInfo -> {
                                String packageName = fileInfo.relativePath().getParent().toString().replace('/', '.');
                                if (!Java.isValidPackageName(packageName)) {
                                    throw new IllegalStateException("failed to deduce package name from source file: " + fileInfo.path());
                                }
                                return packageName;
                            })
                            .distinct())
                .sorted()
                .distinct()
                .map(directory -> directory.replace('/', '.'))
                .collect(Collectors.toList());
    }
}
