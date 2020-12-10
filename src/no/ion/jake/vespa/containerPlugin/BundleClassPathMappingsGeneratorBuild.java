package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.bundle.GenerateBundleClassPathMappings;
import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.build.JavaModule;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class BundleClassPathMappingsGeneratorBuild implements Build {
    private final String name;
    private final ModuleContext moduleContext;
    private final JavaModule module;
    private final List<MavenArtifact> mavenArtifactsForCompile;
    private final Artifact<Path> mappingsFileArtifact;

    public BundleClassPathMappingsGeneratorBuild(String name, ModuleContext moduleContext, JavaModule module,
                                                 List<MavenArtifact> mavenArtifactsForCompile,
                                                 Artifact<Path> mappingsFileArtifact) {
        this.name = name;
        this.moduleContext = moduleContext;
        this.module = module;
        this.mavenArtifactsForCompile = mavenArtifactsForCompile;
        this.mappingsFileArtifact = mappingsFileArtifact;
    }

    @Override public String name() { return name; }

    @Override
    public void build(BuildContext buildContext) {
        ProjectImpl projectAdapter = new ProjectImpl(moduleContext, module,
                mavenArtifactsForCompile.stream().map(MavenArtifact::id).collect(Collectors.toList()));

        var params = new GenerateBundleClassPathMappings.Params(projectAdapter)
                // TODO: jdisc_core_test/test_bundles/cert-ml-dup jdisc_core_test/test_bundles/cert-ml-dup
                // jdisc_core_test/test_bundles/cert-b jdisc_core_test/test_bundles/cert-l1-dup messagebus-disc
                // metrics-proxy container-core container-search-and-docproc hosted-tenant-base provided-dependencies
                // jdisc_core config-model-fat container-messagebus
                .setBundleSymbolicName(buildContext.moduleName());

        GenerateBundleClassPathMappings.execute(params);

        Path outputPath = Path.of("target/test-classes/bundle-plugin.bundle-classpath-mappings.json");
        buildContext.newPublicationOf(mappingsFileArtifact)
                .logWithDuration("wrote " + outputPath)
                .publish(outputPath);
    }
}
