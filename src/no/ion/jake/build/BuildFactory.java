package no.ion.jake.build;

public interface BuildFactory {
    // TODO: Make this return T?  Allows return of all artifacts produced by this method.  Module should extend BuildFactory<Void>.
    void declareBuilds(Declarator declarator);
}
