package no.ion.jake.build;

public class PendingPublication<T> {
    private final T detail;
    private final Artifact<T> artifact;

    public PendingPublication(Artifact<T> artifact, T detail) {
        this.artifact = artifact;
        this.detail = detail;
    }

    public Artifact<T> artifact() { return artifact; }
    public T detail() { return detail; }
    public void publish(BuildContext buildContext) { buildContext.publish(artifact, detail); }
}
