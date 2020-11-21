package no.ion.jake.java;

import no.ion.jake.JakeException;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassPath {
    private final List<Entry> entries;

    private boolean validateExistenceOfPaths = false;

    public ClassPath(ClassPath that) { this(new ArrayList<>(that.entries)); }

    public ClassPath() { this(new ArrayList<>()); }

    private ClassPath(List<Entry> entries) {
        this.entries = entries;
    }

    /** Validate all existing (and future) paths exists. */
    public ClassPath validateExistenceOfPaths() {
        this.validateExistenceOfPaths = true;
        entries.forEach(Entry::validateExistence);
        return this;
    }

    public ClassPath addFrom(ClassPath that) {
        that.entries.forEach(this::addEntry);
        return this;
    }

    public ClassPath addJar(Path path) {
        if (!path.toString().endsWith(".jar")) {
            throw new JakeException("not a *.jar file: " + path);
        }

        return addEntry(Entry.Type.JAR, path);
    }

    public ClassPath addExplodedJar(Path path) {
        return addEntry(Entry.Type.EXPLODED, path);
    }

    private ClassPath addEntry(Entry.Type type, Path path) {
        if (!path.isAbsolute()) {
            throw new JakeException("Class path entries must be absolute");
        }

        String stringPath = path.toString();
        if (stringPath.indexOf(':') != -1) {
            throw new JakeException("class path entry contains ':': " + stringPath);
        }

        return addEntry(new Entry(type, path));
    }

    private ClassPath addEntry(Entry entry) {
        if (validateExistenceOfPaths) {
            entry.validateExistence();
        }
        this.entries.add(entry);
        return this;
    }

    public List<Entry> toList() {
        return List.copyOf(entries);
    }

    public URL[] toUrls() {
        return entries.stream().map(entry -> {
            String url = "file://" + entry.path.toAbsolutePath();
            if (entry.type == Entry.Type.EXPLODED) {
                url += '/';
            }

            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                throw new JakeException(e);
            }
        }).toArray(URL[]::new);
    }

    @Override
    public String toString() {
        return entries.isEmpty() ?
                "." :
                entries.stream().map(entry -> entry.path.toString()).collect(Collectors.joining(":"));
    }

    public static class Entry {
        private final Type type;
        private final Path path;

        public enum Type { JAR, EXPLODED }

        public Entry of(Type type, Path path) {
            if (path.toString().indexOf(':') != -1) {
                throw new IllegalArgumentException("class path entry cannot contain ':': " + path);
            }

            return new Entry(type, path);
        }

        private Entry(Type type, Path path) {
            this.type = type;
            this.path = path;
        }

        public Type type() { return type; }
        public Path path() { return path; }
        public void validateExistence() {
            switch (type) {
                case JAR:
                    if (!Files.isRegularFile(path)) {
                        throw new UncheckedIOException(new NoSuchFileException(path.toString()));
                    }
                    break;
                case EXPLODED:
                    if (!Files.isDirectory(path)) {
                        throw new UncheckedIOException(new NoSuchFileException(path.toString()));
                    }
                    break;
                default:
                    throw new IllegalStateException("type not handled: " + type);
            }
        }
    }
}
