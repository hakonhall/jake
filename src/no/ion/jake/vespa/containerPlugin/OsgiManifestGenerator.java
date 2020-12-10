package no.ion.jake.vespa.containerPlugin;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.build.JavaModule;
import no.ion.jake.build.ModuleContext;
import no.ion.jake.maven.MavenArtifact;

import java.nio.file.Path;
import java.util.List;

public class OsgiManifestGenerator {
    private final String name;
    private final ModuleContext moduleContext;
    private final JavaModule module;

    // TODO: Original has lots of sanity-checking that produces warnings
    private String discApplicationClass = null;
    private String discPreInstallBundle = null;
    private String bundleActivator = null;
    private String jdiscPrivilegedActivator = null;
    private String webInfUrl = null;
    private String mainClass = null;
    private boolean buildLegacyVespaPlatformBundle = false;
    private boolean useArtifactVersionForExportPackages = false;
    private String bundleVersion;
    private String bundleSymbolicName;
    private String importPackage = null;
    private List<MavenArtifact> mavenArtifactsForCompile;
    private Artifact<Path> classesArtifact;

    public OsgiManifestGenerator(String name, ModuleContext moduleContext, JavaModule module) {
        this.name = name;
        this.moduleContext = moduleContext;
        this.module = module;
        this.bundleVersion = module.mavenArtifactId().version();
        this.bundleSymbolicName = module.mavenArtifactId().artifactId();
    }

    // TODO: container-disc and standalone-container
    public OsgiManifestGenerator setDiscApplicationClass(String discApplicationClass) {
        this.discApplicationClass = discApplicationClass;
        return this;
    }

    // TODO: jdisc_http_service, container-disc, and standalone-container define discPreInstallBundle
    public OsgiManifestGenerator setDiscPreInstallBundle(String discPreInstallBundle) {
        this.discPreInstallBundle = discPreInstallBundle;
        return this;
    }

    // TODO: Some tests below jdisc_core_test/test_bundles
    public OsgiManifestGenerator setBundleActivator(String bundleActivator) {
        this.bundleActivator = bundleActivator;
        return this;
    }

    // TODO: Some tests below jdisc_core_test/test_bundles
    public OsgiManifestGenerator setJdiscPrivilegedActivator(String jdiscPrivilegedActivator) {
        this.jdiscPrivilegedActivator = jdiscPrivilegedActivator;
        return this;
    }

    // TODO: bundle-plugin-test/test-bundles/main and controller-server
    public OsgiManifestGenerator setWebInfUrl(String webInfUrl) {
        this.webInfUrl = webInfUrl;
        return this;
    }

    // TODO: metrics, config, configgen, component, config-model, configserver, vespa_feed_perf, socket_test,
    //  vespa-http-client, zookeeper-command-line-client, logserver, document.
    public OsgiManifestGenerator setMainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    // TODO: jdisc_http_service, container-disc, standalone-container
    public OsgiManifestGenerator setBuildLegacyVespaPlatformBundle(boolean buildLegacyVespaPlatformBundle) {
        this.buildLegacyVespaPlatformBundle = buildLegacyVespaPlatformBundle;
        return this;
    }

    // TODO: bundle-plugin-test/test-bundles/artifact-version-for-exports and bundle-plugin-test/test-bundles/artifact-version-for-exports-dep
    public OsgiManifestGenerator setUseArtifactVersionForExportPackages(boolean useArtifactVersionForExportPackages) {
        this.useArtifactVersionForExportPackages = useArtifactVersionForExportPackages;
        return this;
    }

    // TODO: provided-dependencies and config-model-fat
    public OsgiManifestGenerator setBundleVersion(String bundleVersion) {
        this.bundleVersion = bundleVersion;
        return this;
    }

    // TODO: modules under jdisc_core_test/test_bundles, messagebus-disc, metrics-proxy, container-core, container-search-and-docproc,
    //  hosted-tenant-base, provided-dependencies, jdisc_core, config-model-fat, container-messagebus
    public OsgiManifestGenerator setBundleSymbolicName(String bundleSymbolicName) {
        this.bundleSymbolicName = bundleSymbolicName;
        return this;
    }

    // TODO: jdisc_core_test/test_bundles/cert-ml-dup, jdisc_core_test/test_bundles/cert-ml-dup, jdisc_core_test/test_bundles/app-h-log,
    //  jdisc_core_test/test_bundles/app-h-log, jdisc_core_test/test_bundles/cert-q-frag, jdisc_core_test/test_bundles/cert-q-frag,
    //  jdisc_core_test/test_bundles/cert-ml, jdisc_core_test/test_bundles/cert-ml, bundle-plugin-test/test-bundles/main,
    //  bundle-plugin-test/test-bundles/main, messagebus-disc, messagebus-disc, metrics-proxy, config-model-fat, config-model-fat,
    //  config-model-fat, zookeeper-server, zookeeper-server/zookeeper-server-3.5.8, zookeeper-server/zookeeper-server-3.5.6,
    //  zookeeper-server/zookeeper-server-common, zookeeper-server/zookeeper-server-3.5.7, zkfacade
    public OsgiManifestGenerator setImportPackage(String importPackage) {
        this.importPackage = importPackage;
        return this;
    }

    public OsgiManifestGenerator addClassPathEntryArtifacts(List<MavenArtifact> mavenArtifactsForCompile) {
        this.mavenArtifactsForCompile = mavenArtifactsForCompile;
        return this;
    }

    public OsgiManifestGenerator addClassesArtifact(Artifact<Path> classesArtifact) {
        this.classesArtifact = classesArtifact;
        return this;
    }

    public Artifact<Path> declareGenerate(Declarator declarator) {
        try (var declaration = declarator.declareNewBuild()) {
            declaration.dependsOn(classesArtifact);
            // Skip the following, because of the above.
            // mavenArtifactsForCompile.stream().map(MavenArtifact::pathArtifact).forEach(declaration::dependsOn);
            // TODO: depend on resources being copied to classes

            Artifact<Path> manifestArtifact = declaration.producesArtifact(Path.class, "manifest");

            declaration.forBuild(new OsgiManifestGeneratorBuild(
                    name,
                    moduleContext,
                    module,
                    discApplicationClass,
                    discPreInstallBundle,
                    bundleActivator,
                    jdiscPrivilegedActivator,
                    webInfUrl,
                    mainClass,
                    buildLegacyVespaPlatformBundle,
                    useArtifactVersionForExportPackages,
                    bundleVersion,
                    bundleSymbolicName,
                    importPackage,
                    mavenArtifactsForCompile,
                    manifestArtifact));

            return manifestArtifact;
        }
    }
}
