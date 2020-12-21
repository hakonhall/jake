package no.ion.jake.build;

public interface Declarator {
    ModuleContext moduleContext();
    String moduleName();

    default BuildDeclaration declareNewBuild() { return declareNewBuild(moduleName()); }
    BuildDeclaration declareNewBuild(String namespace);

    interface BuildDeclaration extends AutoCloseable {
        BuildDeclaration forBuild(Build build);
        BuildDeclaration dependsOn(Artifact<?> artifact);
        <T> Artifact<T> producesArtifact(Class<T> type, String name);

        @Override
        void close();
    }
}
