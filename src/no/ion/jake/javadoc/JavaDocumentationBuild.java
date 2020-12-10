package no.ion.jake.javadoc;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.io.FileSet;
import no.ion.jake.io.PathPattern;
import no.ion.jake.java.ClassPathEntry;
import no.ion.jake.java.JavaCompilerBuild;
import no.ion.jake.maven.MavenArtifactId;
import no.ion.jake.util.Java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static no.ion.jake.util.Exceptions.uncheckIO;

public class JavaDocumentationBuild implements Build {
    private final String name;
    private final Javadoc javadoc;
    private final List<ClassPathEntry> classPathEntries;
    private final List<Artifact<Path>> sourceDirectoryArtifacts;
    private final List<String> extraArguments;
    private final Path destinationDirectory;
    private final MavenArtifactId mavenArtifactId;
    private final String header;
    private final String bottom;
    private final Artifact<Path> documentationArtifact;

    public JavaDocumentationBuild(String name, Javadoc javadoc, List<ClassPathEntry> classPathEntries,
                                  List<Artifact<Path>> sourceDirectoryArtifacts, List<String> extraArguments, Path destinationDirectory,
                                  MavenArtifactId mavenArtifactId, String header, String bottom, Artifact<Path> documentationArtifact) {
        this.name = name;
        this.javadoc = javadoc;
        this.classPathEntries = requireNonNull(classPathEntries);
        this.sourceDirectoryArtifacts = requireNonNull(sourceDirectoryArtifacts);
        this.extraArguments = requireNonNull(extraArguments);
        this.destinationDirectory = requireNonNull(destinationDirectory);
        this.mavenArtifactId = requireNonNull(mavenArtifactId);
        this.header = requireNonNull(header);
        this.bottom = requireNonNull(bottom);
        this.documentationArtifact = documentationArtifact;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void build(BuildContext buildContext) {
        var arguments = new ArrayList<String>();

        arguments.add("-cp");
        arguments.add(JavaCompilerBuild.makeClassPathString(classPathEntries));

        String sourcePath = sourceDirectoryArtifacts.stream()
                .map(Artifact::detail)
                .map(buildContext.moduleContext()::resolve)
                .filter(Files::isDirectory)
                .map(Path::toString)
                .collect(Collectors.joining(":"));
        if (sourcePath.isEmpty()) {
            // TODO: This should be supported - return an artifact so downstream can avoid making javadoc jar
            throw new IllegalStateException("no source directories");
        }
        arguments.add("--source-path");
        arguments.add(sourcePath);

        Path destinationPath = buildContext.moduleContext().resolve(destinationDirectory);
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

        String title = buildContext.moduleName() + " " + mavenArtifactId.version() + " API";
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
        List<String> packages = findPackages(buildContext);
        arguments.addAll(packages);

        buildContext.log().debug(() -> "javadoc " + String.join(" ", arguments));

        Javadoc.Result result = javadoc.run(arguments);

        if (result.code() != 0) {
            throw new JavaDocumentationException(result.out());
        }

        String warning = result.out();
        if (!warning.isEmpty()) {
            buildContext.log().warning(warning);
        }

        buildContext.newPublicationOf(documentationArtifact)
                .logWithDuration("wrote javadoc to " + destinationDirectory)
                .publish(destinationDirectory);
    }

    /**
     * Returns the packages defined in the source directory, in sorted order.
     *
     * <p>Warning: Calculated based on which source directories have *.java files, which is a heuristic and strictly
     * speaking incorrect as the package of a source file can be unrelated to its path.</p>
     * @param buildContext
     */
    private List<String> findPackages(BuildContext buildContext) {
        return sourceDirectoryArtifacts.stream()
                .map(Artifact::detail)
                .map(buildContext.moduleContext()::resolve)
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
