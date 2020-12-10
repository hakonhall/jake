package no.ion.jake.vespa.containerPlugin;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.build.JavaModule;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;
import java.util.List;

public class BundleClassPathMappingsGenerator {
    private final ModuleContext moduleContext;
    private final JavaModule module;
    private final List<MavenArtifact> mavenArtifactsForCompile;

    public BundleClassPathMappingsGenerator(ModuleContext moduleContext, JavaModule module, List<MavenArtifact> mavenArtifactsForCompile) {
        this.moduleContext = moduleContext;
        this.module = module;
        this.mavenArtifactsForCompile = mavenArtifactsForCompile;
    }

    public Artifact<Path> declareGenerate(Declarator declarator) {
        try (var declaration = declarator.declareNewBuild()) {
            // Although the mappings file includes a path to the destination directory of the source compilation
            // (target/classes), we don't actually depend on compilation having completed or even started yet.

            mavenArtifactsForCompile.stream().map(MavenArtifact::pathArtifact).forEach(declaration::dependsOn);

            Artifact<Path> mappingsFileArtifact = declaration.producesArtifact(Path.class, "bundle classpath mappings");

            declaration.forBuild(new BundleClassPathMappingsGeneratorBuild("generate bundle classpath mappings file",
                    moduleContext, module, mavenArtifactsForCompile, mappingsFileArtifact));

            return mappingsFileArtifact;
        }
    }
}
