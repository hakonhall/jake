package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.osgi.GenerateOsgiManifest;
import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.build.JavaModule;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class OsgiManifestGeneratorBuild implements Build {
    private final String name;
    private final ModuleContext moduleContext;
    private final JavaModule module;
    private final String discApplicationClass;
    private final String discPreInstallBundle;
    private final String bundleActivator;
    private final String jdiscPrivilegedActivator;
    private final String webInfUrl;
    private final String mainClass;
    private final boolean buildLegacyVespaPlatformBundle;
    private final boolean useArtifactVersionForExportPackages;
    private final String bundleVersion;
    private final String bundleSymbolicName;
    private final String importPackage;
    private final List<MavenArtifact> mavenArtifactsForCompile;
    private final Artifact<Path> manifestArtifact;

    public OsgiManifestGeneratorBuild(String name, ModuleContext moduleContext, JavaModule module,
                                      String discApplicationClass, String discPreInstallBundle, String bundleActivator,
                                      String jdiscPrivilegedActivator, String webInfUrl, String mainClass,
                                      boolean buildLegacyVespaPlatformBundle, boolean useArtifactVersionForExportPackages,
                                      String bundleVersion, String bundleSymbolicName, String importPackage,
                                      List<MavenArtifact> mavenArtifactsForCompile, Artifact<Path> manifestArtifact) {
        this.name = name;
        this.moduleContext = moduleContext;
        this.module = module;
        this.discApplicationClass = discApplicationClass;
        this.discPreInstallBundle = discPreInstallBundle;
        this.bundleActivator = bundleActivator;
        this.jdiscPrivilegedActivator = jdiscPrivilegedActivator;
        this.webInfUrl = webInfUrl;
        this.mainClass = mainClass;
        this.buildLegacyVespaPlatformBundle = buildLegacyVespaPlatformBundle;
        this.useArtifactVersionForExportPackages = useArtifactVersionForExportPackages;
        this.bundleVersion = bundleVersion;
        this.bundleSymbolicName = bundleSymbolicName;
        this.importPackage = importPackage;
        this.mavenArtifactsForCompile = mavenArtifactsForCompile;
        this.manifestArtifact = manifestArtifact;
    }

    @Override public String name() { return name; }

    @Override
    public void build(BuildContext buildContext) {
        ProjectImpl projectAdapter = new ProjectImpl(moduleContext, module,
                mavenArtifactsForCompile.stream().map(MavenArtifact::id).collect(Collectors.toList()));
        LogImpl logImpl = new LogImpl(buildContext);

        GenerateOsgiManifest.Params params = new GenerateOsgiManifest.Params(projectAdapter, logImpl)
                .setDiscApplicationClass(discApplicationClass)
                .setDiscPreInstallBundle(discPreInstallBundle)
                .setBundleActivator(bundleActivator)
                .setJdiscPrivilegedActivator(jdiscPrivilegedActivator)
                .setWebInfUrl(webInfUrl)
                .setMainClass(mainClass)
                .setBuildLegacyVespaPlatformBundle(buildLegacyVespaPlatformBundle)
                .setUseArtifactVersionForExportPackages(useArtifactVersionForExportPackages)
                .setBundleVersion(bundleVersion)
                .setBundleSymbolicName(bundleSymbolicName)
                .setImportPackage(importPackage);

        GenerateOsgiManifest.execute(params);

        Path manifestPath = Path.of("target/classes/META-INF/MANIFEST.MF");

        buildContext.newPublicationOf(manifestArtifact)
                .logWithDuration("wrote " + manifestPath)
                .publish(manifestPath);
    }
}
