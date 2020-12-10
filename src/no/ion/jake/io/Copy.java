package no.ion.jake.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributes;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class Copy {
    private final Path fromPath;
    private final Path toPath;

    enum PathType { DIRECTORY, FILE }
    private final PathType type;

    public static Copy copyContents(Path fromDirectory, Path toDirectory) {
        return new Copy(fromDirectory, toDirectory, PathType.DIRECTORY);
    }

    public static Copy copyFileToDirectory(Path fromFile, Path toDirectory) {
        return copyFile(fromFile, toDirectory.resolve(fromFile.getFileName()));
    }

    public static Copy copyFile(Path fromFile, Path toFile) {
        return new Copy(fromFile, toFile, PathType.FILE);
    }

    private Copy(Path fromPath, Path toPath, PathType type) {
        this.fromPath = fromPath;
        this.toPath = toPath;
        this.type = type;
    }

    public CopyResult install() {
        if (!Files.exists(fromPath)) {
            throw new UncheckedIOException(new NoSuchFileException(fromPath.toString()));
        }

        if (type == PathType.FILE) {
            uncheckIO(() -> Files.createDirectories(toPath.getParent()));
            uncheckIO(() -> Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING));
            return CopyResult.copiedFiles(1);
        } else {
            int numCopied = recursiveInstall(fromPath, toPath);
            return CopyResult.copiedFiles(numCopied);
        }
    }

    private static int recursiveInstall(Path from, Path to) {
        // This starts by trying to create the directory, and if FileExist is thrown tests to see if the directory
        // exists.  So this is close to optimal already.
        uncheckIO(() -> Files.createDirectories(to));

        int numCopied = 0;

        DirectoryStream<Path> directoryStream = uncheckIO(() -> Files.newDirectoryStream(from));
        try {
            for (var path : directoryStream) {
                var toPath = to.resolve(path.getFileName());

                var attributes = uncheckIO(() -> Files.readAttributes(path, PosixFileAttributes.class));
                if (attributes.isDirectory()) {
                    numCopied += recursiveInstall(from.resolve(path), toPath);
                } else if (attributes.isRegularFile()) {
                    try {
                        PosixFileAttributes toAttributes = Files.readAttributes(toPath, PosixFileAttributes.class);
                        if (attributes.lastModifiedTime().compareTo(toAttributes.lastModifiedTime()) <= 0) {
                            continue;
                        }
                    } catch (NoSuchFileException e) {
                        // fall through
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    uncheckIO(() -> Files.copy(path, toPath, StandardCopyOption.REPLACE_EXISTING));
                    ++numCopied;
                }
            }
        } finally {
            uncheckIO(directoryStream::close);
        }

        return numCopied;
    }
}
