package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.api.Artifact2;
import com.yahoo.container.plugin.api.Project;
import com.yahoo.container.plugin.api.Version2;
import no.ion.jake.module.ModuleContext;
import no.ion.jake.java.ClassPathBuilder;
import no.ion.jake.maven.MavenArtifact;
import no.ion.jake.maven.Scope;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectImpl implements Project {
    private final ModuleContext moduleContext;
    private final ClassPathBuilder classPathBuilder;
    private final EnumSet<Scope> scopesToInclude;

    public ProjectImpl(ModuleContext moduleContext, ClassPathBuilder classPathBuilder) {
        this.moduleContext = moduleContext;
        this.classPathBuilder = classPathBuilder;
        this.scopesToInclude = EnumSet.copyOf(ClassPathBuilder.COMPILE_SCOPES);
    }

    public void setScopesToInclude(EnumSet<Scope> set) {
        scopesToInclude.clear();
        scopesToInclude.addAll(set);
    }

    @Override
    public String getName() {
        // TODO: Not entirely identical (artifactId vs name in Maven)
        return moduleContext.name();
    }

    @Override
    public String getBuildDirectory() {
        return moduleContext.path().resolve("target").toString();
    }

    @Override
    public String getBuildFinalName() {
        // TODO: May not be identical for all modules?
        MavenArtifact artifactId = moduleContext.mavenArtifact();
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
        return classPathBuilder.getMavenDependencies(scopesToInclude)
                .stream()
                .map(mavenArtifact -> new Artifact2Impl(mavenArtifact, moduleContext.project().pathToMavenRepository()))
                .collect(Collectors.toList());
    }

    @Override
    public Version2 parseVersion(String s) {
        throw new UnsupportedOperationException("Fix parsing of: " + s);
    }
}
