package no.ion.jake.vespa.abiCheckPlugin;

import com.yahoo.abicheck.ProjectApi;
import no.ion.jake.ModuleContext;

import java.io.File;
import java.nio.file.Path;

public class ProjectImpl implements ProjectApi {
    private final ModuleContext moduleContext;
    private final Path absoluteJarPath;

    ProjectImpl(ModuleContext moduleContext, Path jarPath) {
        this.moduleContext = moduleContext;
        this.absoluteJarPath = jarPath.isAbsolute() ? jarPath : moduleContext.path().resolve(jarPath);
    }

    @Override
    public File jarFile() {
        return absoluteJarPath.toFile();
    }

    @Override
    public File getBasedir() {
        return moduleContext.path().toFile();
    }
}
