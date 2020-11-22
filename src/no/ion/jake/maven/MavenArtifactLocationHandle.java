package no.ion.jake.maven;

import java.nio.file.Path;

public interface MavenArtifactLocationHandle {
    /** Returns the original MavenArtifact. */
    MavenArtifact mavenArtifact();

    /**
     * @return the path of the Maven artifact.
     * @throws IllegalStateException if this is invoked before it is available.
     */
    Path path();
}
