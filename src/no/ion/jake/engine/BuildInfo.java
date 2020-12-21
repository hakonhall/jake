package no.ion.jake.engine;

import no.ion.jake.build.Build;
import no.ion.jake.build.Module;
import no.ion.jake.build.ModuleContext;

import java.util.Set;

public class BuildInfo {
    private final BuildId id;
    private final ModuleContext moduleContext;
    private final String namespace;
    private final Build build;
    private final Set<ArtifactId> dependencies;
    private final Set<ArtifactId> production;
    private final Set<BuildId> buildDependencies;

    public BuildInfo(BuildId id, ModuleContext moduleContext, String namespace, Build build, Set<ArtifactId> dependencies,
                     Set<ArtifactId> production, Set<BuildId> buildDependencies) {
        this.id = id;
        this.moduleContext = moduleContext;
        this.namespace = namespace;
        this.build = build;
        this.dependencies = dependencies;
        this.production = production;
        this.buildDependencies = buildDependencies;
    }

    public BuildId id() { return id; }
    public ModuleContext moduleContext() { return moduleContext; }
    public String namespace() { return namespace; }
    public Build build() { return build; }
    public Set<ArtifactId> dependencies() { return dependencies; }
    public Set<ArtifactId> production() { return production; }
    public Set<BuildId> buildDependencies() { return buildDependencies; }
}
