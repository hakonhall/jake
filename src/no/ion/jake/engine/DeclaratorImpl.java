package no.ion.jake.engine;

import no.ion.jake.build.Declarator;
import no.ion.jake.build.Module;
import no.ion.jake.build.ModuleContext;

public class DeclaratorImpl implements Declarator {
    private final BuildSet buildSet;
    private final ModuleContext moduleContext;
    private final Module module;

    public DeclaratorImpl(BuildSet buildSet, ModuleContext moduleContext, Module module) {
        this.buildSet = buildSet;
        this.moduleContext = moduleContext;
        this.module = module;
    }

    @Override public ModuleContext moduleContext() { return moduleContext; }
    @Override public String moduleName() { return module.moduleName(); }

    @Override
    public BuildDeclarationImpl declareNewBuild() {
        return new BuildDeclarationImpl(buildSet, moduleContext, module.moduleName());
    }

    @Override
    public BuildDeclaration declareNewBuild(String namespace) {
        return new BuildDeclarationImpl(buildSet, moduleContext, namespace);
    }
}
