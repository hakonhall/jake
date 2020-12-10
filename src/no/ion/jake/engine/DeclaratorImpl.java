package no.ion.jake.engine;

import no.ion.jake.build.Declarator;
import no.ion.jake.build.Module;
import no.ion.jake.build.ModuleContext;

public class DeclaratorImpl implements Declarator {
    private final BuildGraph buildGraph;
    private final ModuleContext moduleContext;
    private final Module module;

    public DeclaratorImpl(BuildGraph buildGraph, ModuleContext moduleContext, Module module) {
        this.buildGraph = buildGraph;
        this.moduleContext = moduleContext;
        this.module = module;
    }

    @Override
    public ModuleContext moduleContext() {
        return moduleContext;
    }

    @Override
    public BuildDeclarationImpl declareNewBuild() {
        return new BuildDeclarationImpl(buildGraph, moduleContext, module);
    }

    @Override
    public BuildDeclarationImpl declareGlobalBuild() {
        // todo
        return declareNewBuild();
    }

}
