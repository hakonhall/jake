package no.ion.jake;

public interface Module {
    /** Allows a module to declare which targets it produces.  It may restrict its set to those limited by Target. */
    void registerTargets(ModuleContext moduleContext, TargetPattern targets);
}
