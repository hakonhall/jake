package no.ion.jake.vespa.containerPlugin;

import no.ion.jake.BuildContext;
import no.ion.jake.java.ClassPathBuilder;
import no.ion.jake.java.Jar;
import no.ion.jake.java.JavaArchiver;
import no.ion.jake.java.JavaArchiverResult;

import java.nio.file.Path;

/**
 * Based on the bundle-plugin's AssembleContainerPlugin.
 */
public class Assembler {
    private final Jar jar;
    private final ClassPathBuilder classPathBuilder;

    private boolean useCommonAssemblyIds = true;  // true by parent/pom.xml override
    private boolean attachBundleArtifact = false;
    private String bundleClassifierName = "bundle";
    private JavaArchiver withoutArchiver = null;

    public Assembler(Jar jar, ClassPathBuilder classPathBuilder) {
        this.jar = jar;
        this.classPathBuilder = classPathBuilder;
    }

    // TODO: tenant-cd-api, controller-server, controller-api, cloud-tenant-cd, vespa-osgi-testrunner, parent (i.e. default)
    public Assembler setUseCommonAssemblyIds(boolean useCommonAssemblyIds) {
        this.useCommonAssemblyIds = useCommonAssemblyIds;
        return this;
    }

    // TODO: bundle-plugin-test/test-bundles, controller-server, docker-api, controller-api
    public Assembler setAttachBundleArtifact(boolean attachBundleArtifact) {
        this.attachBundleArtifact = attachBundleArtifact;
        return this;
    }

    // TODO: controller-server, controller-api
    public Assembler setBundleClassifierName(String bundleClassifierName) {
        this.bundleClassifierName = bundleClassifierName;
        return this;
    }

    public void build(BuildContext buildContext) {
        final String withoutSuffix;
        final String withSuffix;
        if (useCommonAssemblyIds) {
            withoutSuffix = ".jar";
            withSuffix = "-jar-with-dependencies.jar";
        } else {
            withoutSuffix = "-without-dependencies.jar";
            withSuffix = "-deploy.jar";
        }

        withoutArchiver = JavaArchiver.forCreating(buildContext.moduleContext(), jar)
                .setPath("target/" + buildContext.moduleContext().mavenArtifact().artifactId() + withoutSuffix)
                .setManifestPath(classPathBuilder.getCompileDestinationDirectory().resolve("META-INF/MANIFEST.MF"))
                .includeDirectory(classPathBuilder.getCompileDestinationDirectory());

        buildContext.run(withoutArchiver);

        JavaArchiver withArchiver = JavaArchiver.forCreating(buildContext.moduleContext(), jar)
                .setPath("target/" + buildContext.moduleContext().mavenArtifact().artifactId() + withSuffix)
                .setManifestPath(classPathBuilder.getCompileDestinationDirectory().resolve("META-INF/MANIFEST.MF"))
                .includeDirectory(classPathBuilder.getCompileDestinationDirectory())
                // TODO: Include compile scoped jars in dependencies/, and add to Bundle-ClassPath
                ;

        buildContext.run(withArchiver);
    }

    public Path getJarPath() {
        return withoutArchiver.path();
    }
}
