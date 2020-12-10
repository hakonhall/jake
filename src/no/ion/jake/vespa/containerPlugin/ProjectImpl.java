package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.api.Artifact2;
import com.yahoo.container.plugin.api.Project;
import com.yahoo.container.plugin.api.Version2;
import no.ion.jake.build.JavaModule;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.maven.MavenArtifactId;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectImpl implements Project {
    private final ModuleContext moduleContext;
    private final JavaModule module;
    private final List<MavenArtifactId> mavenArtifactIdsForCompile;

    public ProjectImpl(ModuleContext moduleContext, JavaModule module, List<MavenArtifactId> mavenArtifactIdsForCompile) {
        this.moduleContext = moduleContext;
        this.module = module;
        this.mavenArtifactIdsForCompile = mavenArtifactIdsForCompile;
    }

    @Override
    public String getName() {
        // TODO: Not entirely identical (artifactId vs name in Maven)
        return module.moduleName();
    }

    @Override
    public String getBuildDirectory() {
        return moduleContext.path().resolve("target").toString();
    }

    @Override
    public String getBuildFinalName() {
        // TODO: May not be identical for all modules?
        MavenArtifactId artifactId = module.mavenArtifactId();
        return artifactId.groupId() + ":" + artifactId.artifactId();
    }

    @Override
    public String getBuildOutputDirectory() {
        return moduleContext.path().resolve("target/classes").toString();
    }

    @Override
    public String getBuildTestOutputDirectory() {
        return moduleContext.path().resolve("target/test-classes").toString();
    }

    @Override
    public List<Artifact2> getArtifacts() {
        return mavenArtifactIdsForCompile
                .stream()
                .map(mavenArtifact -> new Artifact2Impl(mavenArtifact, moduleContext.getProject().pathToMavenRepository()))
                .collect(Collectors.toList());
    }

    @Override
    public Version2 parseVersion(String s) {
        throw new UnsupportedOperationException("Fix parsing of: " + s);
    }

}
