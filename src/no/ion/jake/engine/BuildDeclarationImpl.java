package no.ion.jake.engine;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.Declarator;
import no.ion.jake.build.ModuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuildDeclarationImpl implements Declarator.BuildDeclaration {
    private final List<Artifact<?>> dependencies = new ArrayList<>();
    private final Set<ArtifactImpl<?>> production = new HashSet<>();
    private final BuildSet buildSet;
    private final ModuleContext moduleContext;
    private final String namespace;

    private Build build = null;
    private boolean closed = false;

    public BuildDeclarationImpl(BuildSet buildSet, ModuleContext moduleContext, String namespace) {
        this.buildSet = buildSet;
        this.moduleContext = moduleContext;
        this.namespace = namespace;
    }

    @Override
    public BuildDeclarationImpl dependsOn(Artifact<?> artifact) {
        if (closed) {
            throw new IllegalStateException("it's illegal to invoke dependsOn() after close()");
        }

        dependencies.add(artifact);
        return this;
    }

    @Override
    public <T> ArtifactImpl<T> producesArtifact(Class<T> type, String name) {
        return producesArtifact(type, name, namespace);
    }

    private <T> ArtifactImpl<T> producesArtifact(Class<T> type, String name, String namespace) {
        if (closed) {
            throw new IllegalStateException("it's illegal to invoke producesArtifact() after close()");
        }

        ArtifactImpl<T> artifactImpl = buildSet.newArtifact(type, namespace, name);

        if (production.contains(artifactImpl)) {
            throw new IllegalArgumentException("duplicate production of artifact '" + name + "'");
        }
        production.add(artifactImpl);

        return artifactImpl;
    }

    @Override
    public BuildDeclarationImpl forBuild(Build build) {
        if (closed) {
            throw new IllegalStateException("it's illegal to invoke producesArtifact() after close()");
        }

        this.build = build;
        return this;
    }

    @Override
    public void close() {
        if (closed) {
            throw new IllegalStateException("it's illegal to invoke close() twice");
        }
        closed = true;

        if (build == null) {
            throw new IllegalStateException("bindTo() has not been invoked");
        }

        buildSet.addBuild(moduleContext, namespace, build, dependencies, production);
    }
}
