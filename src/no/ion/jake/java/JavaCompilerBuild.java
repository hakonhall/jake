package no.ion.jake.java;

import no.ion.jake.JakeException;
import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.io.FileSet2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class JavaCompilerBuild implements Build {
    private final Javac javac;
    private final String name;
    private final List<Artifact<FileSet2>> sourceFileArtifacts;
    private final List<String> passthroughJavacArguments;
    private final List<ClassPathEntry> classPath;
    private final Path destinationDirectory;
    private final Artifact<Path> destinationDirectoryArtifact;

    public JavaCompilerBuild(Javac javac, String name, List<Artifact<FileSet2>> sourceFileArtifacts,
                             List<String> passthroughJavacArguments, List<ClassPathEntry> classPath, Path destinationDirectory,
                             Artifact<Path> destinationDirectoryArtifact) {
        this.javac = javac;
        this.name = name;
        this.sourceFileArtifacts = List.copyOf(sourceFileArtifacts);
        this.passthroughJavacArguments = List.copyOf(passthroughJavacArguments);
        this.classPath = List.copyOf(classPath);
        this.destinationDirectory = destinationDirectory;
        this.destinationDirectoryArtifact = destinationDirectoryArtifact;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void build(BuildContext buildContext) throws JavaCompilerException {
        var arguments = new ArrayList<String>(passthroughJavacArguments);

        arguments.add("-cp");
        arguments.add(makeClassPathString(classPath));

        if (destinationDirectory == null) {
            throw new JavaCompilerException("destination directory not set");
        }
        Path resolvedDestinationDirectory = buildContext.moduleContext().resolve(destinationDirectory);
        arguments.add("-d");
        arguments.add(resolvedDestinationDirectory.toString());

        uncheckIO(() -> Files.createDirectories(resolvedDestinationDirectory));

        List<String> sourceFiles = resolveSourceFiles(sourceFileArtifacts);
        if (sourceFiles.isEmpty()) {
            buildContext.newPublicationOf(destinationDirectoryArtifact)
                    .log("compiled no files")
                    .publish(resolvedDestinationDirectory);
            return;
        }
        arguments.addAll(sourceFiles);

        buildContext.log().debug(() -> "javac " + String.join(" ", arguments));

        Javac.CompileResult result = javac.compile(arguments);

        if (result.code != 0) {
            throw new JavaCompilerException(result.message);
        }

        if (result.message != null && !result.message.isBlank()) {
            buildContext.log().warning(result.message);
        }

        int numFilesCompiled = sourceFiles.size();
        buildContext.newPublicationOf(destinationDirectoryArtifact)
                .logWithDuration(String.format("compiled %d file%s to %s",
                        numFilesCompiled,
                        numFilesCompiled == 1 ? "" : "s",
                        destinationDirectory))
                .publish(resolvedDestinationDirectory);
    }

    public static String makeClassPathString(List<ClassPathEntry> classPath) {
        String classPathString = classPath.stream()

                .map(ClassPathEntry::getValidatedPath)
                .map(Path::toString)
                .peek(path -> {
                    if (path.contains(":")) {
                        throw new JakeException("class path entry cannot contain ':': " + path);
                    }
                })
                .collect(Collectors.joining(":"));

        return classPathString.isEmpty() ? "." : classPathString;
    }

    private static List<String> resolveSourceFiles(List<Artifact<FileSet2>> sourceFileArtifacts) {
        return sourceFileArtifacts.stream()
                .map(Artifact::detail)
                .flatMap(fileset -> fileset.toPathList().stream())
                .map(Path::toString)
                .collect(Collectors.toList());
    }
}
