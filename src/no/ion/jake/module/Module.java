package no.ion.jake.module;

import no.ion.jake.build.BuildRegistry;

public interface Module {
    /** Allows a module to declare which targets it produces.  It may restrict its set to those limited by Target. */
    void registerTargets(ModuleContext moduleContext, TargetPattern targets);

    void registerBuilds(BuildRegistry registry);
}
