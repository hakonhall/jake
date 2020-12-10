package no.ion.jake.io;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileSet2 {
    private final Map<Path, FileInfo> files;

    public FileSet2(Map<Path, FileInfo> files) {
        this.files = Map.copyOf(files);
    }

    public List<Path> toPathList() {
        return files.values().stream().map(FileInfo::path).collect(Collectors.toList());
    }
}
