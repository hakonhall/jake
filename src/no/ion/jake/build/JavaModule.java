package no.ion.jake.build;

import no.ion.jake.maven.MavenArtifact;
import no.ion.jake.maven.MavenArtifactId;

public interface JavaModule extends Module {
    MavenArtifactId mavenArtifactId();

    /** Must be invoked after declareBuilds() */
    MavenArtifact mavenArtifact();
}
