package no.ion.jake.io;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.build.Declarator;
import no.ion.jake.build.OptionalArtifact;
import no.ion.jake.maven.MavenArtifact;
import no.ion.jake.maven.MavenArtifactId;
import no.ion.jake.maven.MavenRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArtifactInstaller {
    private final Declarator declarator;
    private final MavenRepository mavenRepository;
    private final List<Artifact<?>> dependencies = new ArrayList<>();

    public ArtifactInstaller(Declarator declarator, MavenRepository mavenRepository) {
        this.declarator = declarator;
        this.mavenRepository = mavenRepository;
    }

    public ArtifactInstaller withDependencies(Artifact<?>... artifacts) {
        dependencies.addAll(Arrays.asList(artifacts));
        return this;
    }

    public MavenArtifact install(Artifact<Path> pathArtifact, MavenArtifactId mavenArtifactId) {
        return install(OptionalArtifact.ofArtifact(pathArtifact), mavenArtifactId);
    }

    public MavenArtifact install(Path path, MavenArtifactId mavenArtifactId) {
        return install(OptionalArtifact.ofInstance(path), mavenArtifactId);
    }

    public MavenArtifact install(OptionalArtifact<Path> pathArtifact, MavenArtifactId mavenArtifactId) {
        try (var declaration = declarator.declareNewBuild()) {
            pathArtifact.getArtifact().ifPresent(declaration::dependsOn);
            dependencies.forEach(declaration::dependsOn);

            Artifact<Path> toPathArtifact = declaration.producesArtifact(Path.class, mavenArtifactId.toCoordinate());

            declaration.forBuild(new Build() {
                @Override public String name() { return "install of " + mavenArtifactId.toString(); }

                @Override
                public void build(BuildContext buildContext) {
                    Path fromPath = buildContext.moduleContext().resolve(pathArtifact.get());
                    String repoPath = mavenArtifactId.toRepoPath();
                    Path toPath = mavenRepository.localRepositoryPath().resolve(repoPath);
                    CopyResult result = Copy.copyFile(fromPath, toPath).install();

                    buildContext.newPublicationOf(toPathArtifact)
                            .log("installed " + pathArtifact.get() + " as " + repoPath)
                            .publish(toPath);
                }
            });

            return new MavenArtifact(mavenArtifactId, toPathArtifact);
        }
    }
}
