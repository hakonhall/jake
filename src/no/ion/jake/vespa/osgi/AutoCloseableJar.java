package no.ion.jake.vespa.osgi;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static no.ion.jake.util.Exceptions.uncheckIO;

public class AutoCloseableJar implements AutoCloseable {
    private final JarFile jarFile;

    private Manifest manifest = null;

    public static AutoCloseableJar fromPath(Path path) {
        File file = path.toFile();
        JarFile jarFile = uncheckIO(() -> new JarFile(file));
        return new AutoCloseableJar(jarFile);
    }

    private AutoCloseableJar(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public Manifest manifest() {
        if (manifest == null) {
            manifest = uncheckIO(jarFile::getManifest);
        }
        return manifest;
    }

    public Attributes mainAttributes() {
        return manifest().getMainAttributes();
    }

    public Optional<String> mainAttributeValue(String name) {
        return Optional.ofNullable(mainAttributes().getValue(name));
    }

    public List<Export> getExportPackage() {
        return mainAttributeValue("Export-Package").map(ExportPackageParser::parseExports).orElseGet(List::of);
    }

    @Override
    public void close() {
        uncheckIO(jarFile::close);
    }
}
