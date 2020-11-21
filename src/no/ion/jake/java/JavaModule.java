package no.ion.jake.java;

import no.ion.jake.Project;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;

/**
 * A Java module produces a JAR file, composed of *.class files compiled from Java source files, and other
 * resource files.  Typically runs unit tests to verify the module.
 */
public interface JavaModule {
    /**
     * @return the {@link MavenArtifact} identifying the Java module's JAR output file.
     * @implSpec the JAR must be installed in the local maven repository ({@link Project#pathToMavenRepository()})
     * during build of this module.
     */
    MavenArtifact jarMavenArtifact();
}
