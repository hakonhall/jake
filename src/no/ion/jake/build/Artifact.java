package no.ion.jake.build;

/**
 * The Java handle for the artifact produced by a build, e.g. a JAR file.
 * The name must uniquely identify the artifact instance within the module, by reference equals ==.
 */
public interface Artifact<T> extends Artifacts {
    String name();

    /** Any additional details associated with the artifact.  Use {@link Void} if this artifact is not associated with any instance. */
    default T detail() { throw new IllegalStateException("detail has not been set"); }
}
