package no.ion.jake.build;

public interface Declarator {
    ModuleContext moduleContext();

    BuildDeclaration declareNewBuild();
    BuildDeclaration declareGlobalBuild();

    interface BuildDeclaration extends AutoCloseable {
        BuildDeclaration forBuild(Build build);
        BuildDeclaration dependsOn(Artifact<?> artifact);
        <T> Artifact<T> producesArtifact(Class<T> type, String name);

        <T> Artifact<T> producesGlobalArtifact(Class<T> type, String name);

        @Override
        void close();
    }
}
