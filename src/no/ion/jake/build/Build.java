package no.ion.jake.build;

/** A build can produce its artifacts once its dependencies are satisfied. */
public interface Build {
    /** Unique name of the build within the module. */
    String name();

    /**
     * Builds the artifacts of the factory.
     *
     * <p>May be invoked any number of times when the dependencies are satisfied.</p>
     */
    void build(BuildContext buildContext);

}
