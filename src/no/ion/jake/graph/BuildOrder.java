package no.ion.jake.graph;

public interface BuildOrder<T> {

    interface NextBuild<U> {
        boolean isDone();
        boolean isReady();
        U getReadyBuild();
    }

    NextBuild<T> calculateNextBuild();

    void reportActiveBuild(T build);

    void reportCompletedBuild(T build);
}
