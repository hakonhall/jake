package no.ion.jake.io;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.ion.jake.util.Exceptions.uncheckIO;

/**
 * A set of files.  The set can be expanded to include explicit files, or to include files in a directory tree
 * matching a pattern, etc.  The list of paths can be retrieved, as well as info on the last modified time, etc.
 */
public class FileSet {
    private final Path root;
    private final Map<Path, PathPattern> patternByDirectory = new HashMap<>();
    private final Map<Path, FileInfo> fileByDirectory = new HashMap<>();

    private FileTime firstModification = null;
    private FileTime lastModification = null;

    /** @param root directory to resolve relative paths - otherwise relative paths are not permitted. */
    public FileSet(Path root) {
        this.root = root;
    }

    public FileSet() { this(null); }

    public FileSet includeFiles(String directory) {
        return includeFiles(resolvePath(directory), PathPattern.ofAll());
    }

    public FileSet includeFiles(String directory, PathPattern pattern) {
        return includeFiles(resolvePath(directory), pattern);
    }

    public FileSet includeFiles(Path directory, PathPattern pattern) {
        return includeFilesInternal(resolvePath(directory), pattern);
    }

    private FileSet includeFilesInternal(Path normalizedPath, PathPattern pattern) {
        PathPattern previousPattern = patternByDirectory.put(normalizedPath, pattern);
        if (previousPattern != null) {
            throw new IllegalArgumentException("fileset already includes directory " + normalizedPath);
        }

        return this;
    }

    public FileSet scan() {
        firstModification = null;
        lastModification = null;
        patternByDirectory.forEach(this::findFiles);
        return this;
    }

    public List<FileInfo> toFileInfoList() {
        return List.copyOf(fileByDirectory.values());
    }

    public List<Path> toPathList() {
        return fileByDirectory.values().stream().map(FileInfo::path).collect(Collectors.toList());
    }

    private Path resolvePath(String path) {
        if (root == null) {
            throw new IllegalArgumentException("Unable to resolve string path without a root directory path: " + path);
        }
        return root.resolve(path).normalize();
    }

    private Path resolvePath(Path path) {
        if (path.isAbsolute()) return path.normalize();
        if (root == null) {
            throw new IllegalArgumentException("Unable to resolve relative path without a root directory path: " + path);
        }
        return root.resolve(path).normalize();
    }

    private void findFiles(Path rootDirectory, PathPattern pattern) {
        LinkedList<Path> relativeDirectoriesNotVisited = new LinkedList<>();
        relativeDirectoriesNotVisited.add(rootDirectory.getFileSystem().getPath("."));

        while (!relativeDirectoriesNotVisited.isEmpty()) {
            Path relativeDirectory = relativeDirectoriesNotVisited.pollFirst();
            Path directory = rootDirectory.resolve(relativeDirectory);
            DirectoryStream<Path> stream = uncheckIO(() -> Files.newDirectoryStream(directory));
            try {
                for (Path path : stream) {
                    // path is 'directory.resolve(directoryEntryName)'
                    Path relativePath = relativeDirectory.resolve(path.getFileName());

                    PosixFileAttributes attributes = uncheckIO(() -> Files.readAttributes(path, PosixFileAttributes.class));

                    if (attributes.isRegularFile()) {
                        if (pattern.test(relativePath)) {
                            FileTime lastModifiedTime = attributes.lastModifiedTime();
                            if (firstModification == null || lastModifiedTime.compareTo(firstModification) < 0) {
                                firstModification = lastModification;
                            }
                            if (lastModification == null || lastModification.compareTo(lastModifiedTime) < 0) {
                                lastModification = lastModifiedTime;
                            }

                            FileInfo fileInfo = FileInfo.fromRelativePath(rootDirectory, relativePath.normalize(), lastModifiedTime);
                            fileByDirectory.put(path.normalize(), fileInfo);
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
