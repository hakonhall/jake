package no.ion.jake.build;

import java.time.Duration;

public interface BuildContext {
    ModuleContext moduleContext();

    /** Namespace of the build, typically module name for builds associated with a module (unless overridden). */
    String namespace();
    Logger log();
    Duration durationUpToNow();

    default <T> void publish(Artifact<T> artifact, T detail) { newPublicationOf(artifact).publish(detail); }
    default void publish(Artifact<Void> artifact) { publish(artifact, null); }

    interface Publication<T> {
        Publication<T> logWithDuration(String accomplishment);
        Publication<T> log(String accomplishment);

        Publication<T> hasChanged(boolean hasChanged);
        void publish(T detail);  // use null for Void
    }
    <T> Publication<T> newPublicationOf(Artifact<T> artifact);
}
