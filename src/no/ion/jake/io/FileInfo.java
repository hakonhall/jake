package no.ion.jake.io;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FileInfo {
    private final Path directory;
    private final Path relativePath;
    private final FileTime lastModifiedTime;

    public static FileInfo fromRelativePath(Path directory, Path relativePath, FileTime lastModifiedTime) {
        return new FileInfo(directory, relativePath, lastModifiedTime);
    }

    public static FileInfo fromPath(Path directory, Path path, FileTime lastModifiedTime) {
        return new FileInfo(directory, directory.relativize(path), lastModifiedTime);
    }

    private FileInfo(Path directory, Path relativePath, FileTime lastModifiedTime) {
        this.directory = directory;
        this.relativePath = relativePath;
        this.lastModifiedTime = lastModifiedTime;

        if (!directory.isAbsolute()) {
            throw new IllegalArgumentException("directory is not absolute: " + directory);
        }
    }

    public Path directory() { return directory; }
    public Path path() { return directory.resolve(relativePath); }
    public Path relativePath() { return relativePath; }
    public FileTime lastModifiedTime() { return lastModifiedTime; }
}
