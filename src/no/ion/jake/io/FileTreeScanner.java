package no.ion.jake.io;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.build.Declarator;

import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static no.ion.jake.util.Exceptions.uncheckIO;

/**
 * A module should use a FileTreeScanner.BuildDeclaration to create a FileTreeScanner, which produces FileSet2.
 *
 * A set of files.  The set can be expanded to include explicit files, or to include files in a directory tree
 * matching a pattern, etc.  The list of paths can be retrieved, as well as info on the last modified time, etc.
 */
public class FileTreeScanner implements Build {
    private final String name;
    private final Map<Path, IncludeSpec> includes;
    private final Artifact<FileSet2> fileSetArtifact;

    private Map<Path, FileInfo> files = null;

    /**
     * @param name         a name that can be used to describe the file set, e.g. "source" or "test source".
     *                     Will be used as the name of the artifact, and must be unique among all artifacts declared
     *                     by the module.
     */
    public static Builder newBuilder(Declarator declarator, String name) {
        return new Builder(declarator, name);
    }

    public static class Builder {
        private final Map<Path, IncludeSpec> includes = new HashMap<>();
        private final String name;
        private final Declarator declarator;

        private Builder(Declarator declarator, String name) {
            this.name = name;
            this.declarator = declarator;
        }

        public Builder includeFiles(Path directory, boolean mustExist, PathPattern pattern) {
            includes.put(directory, new IncludeSpec(directory, mustExist, pattern));
            return this;
        }

        public Artifact<FileSet2> declareScan() {
            try (Declarator.BuildDeclaration buildDeclaration = declarator.declareNewBuild()) {
                Artifact<FileSet2> fileSetArtifact = buildDeclaration.producesArtifact(FileSet2.class, name);
                buildDeclaration.forBuild(new FileTreeScanner(name, includes, fileSetArtifact));
                return fileSetArtifact;
            }
        }
    }

    private FileTreeScanner(String name, Map<Path, IncludeSpec> includes, Artifact<FileSet2> fileSetArtifact) {
        this.name = name;
        this.includes = includes;
        this.fileSetArtifact = fileSetArtifact;
    }

    @Override public String name() { return "finding " + name; }

    @Override
    public void build(BuildContext buildContext) {
        scan(buildContext);
        FileSet2 fileSet = new FileSet2(Map.copyOf(files));
        buildContext.newPublicationOf(fileSetArtifact)
                .logWithDuration("found " + files.size() + " " + name + " files")
                .publish(fileSet);
    }

    private static class IncludeSpec {
        public final Path directory;
        public final boolean mustExist;
        public final PathPattern pattern;

        public IncludeSpec(Path directory, boolean mustExist, PathPattern pattern) {
            this.directory = directory;
            this.pattern = pattern;
            this.mustExist = mustExist;
        }
    }

    private void scan(BuildContext buildContext) {
        Map<Path, FileInfo> files = new HashMap<>();
        includes.forEach((path, includeSpec) -> findFiles(buildContext, includeSpec, files));
        this.files = files;
    }

    private void findFiles(BuildContext buildContext, IncludeSpec includeSpec, Map<Path, FileInfo> files) {
        // includeSpec.directory could be e.g. "src/main/java", while baseDirectory is that path resolved with the path
        // of the module directory (which may be absolute (or relative?)).
        Path resolvedDirectory = buildContext.moduleContext().resolve(includeSpec.directory);
        boolean directoryExists = Files.isDirectory(resolvedDirectory);
        if (!directoryExists) {
            if (!includeSpec.mustExist) {
                return;
            }

            throw new UncheckedIOException(new NoSuchFileException(resolvedDirectory.toString()));
        }

        LinkedList<Path> relativeDirectoriesNotVisited = new LinkedList<>();
        relativeDirectoriesNotVisited.add(buildContext.moduleContext().pathOf("."));

        while (!relativeDirectoriesNotVisited.isEmpty()) {
            Path relativeDirectory = relativeDirectoriesNotVisited.pollFirst();
            Path directory = resolvedDirectory.resolve(relativeDirectory);
            DirectoryStream<Path> stream = uncheckIO(() -> Files.newDirectoryStream(directory));
            try {
                for (Path path : stream) {
                    PosixFileAttributes attributes = uncheckIO(() -> Files.readAttributes(path, PosixFileAttributes.class));

                    // path is 'directory.resolve(directoryEntryName)'
                    Path relativePath = relativeDirectory.resolve(path.getFileName());

                    if (attributes.isRegularFile()) {
                        if (includeSpec.pattern.test(relativePath)) {
                            FileTime lastModifiedTime = attributes.lastModifiedTime();

                            FileInfo fileInfo = FileInfo.fromRelativePath(resolvedDirectory, relativePath.normalize(),
                                    lastModifiedTime);
                            files.put(path.normalize(), fileInfo);
                        }
                    } else if (attributes.isDirectory()) {
                        relativeDirectoriesNotVisited.add(relativePath);
                    } // ignore attributes.isOther()
                }
            } finally {
                uncheckIO(stream::close);
            }
        }
    }
}
