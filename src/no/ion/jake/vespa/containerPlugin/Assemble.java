package no.ion.jake.vespa.containerPlugin;

import no.ion.jake.build.Artifact;
import no.ion.jake.build.Declarator;
import no.ion.jake.build.JavaModule;
import no.ion.jake.java.Jar;
import no.ion.jake.java.JavaArchiver;

import java.nio.file.Path;

public class Assemble {
    private final Jar jar;
    private final JavaModule module;
    private final Artifact<Path> classesArtifact;
    private final Artifact<Path> manifestArtifact;

    private boolean useCommonAssemblyIds = true;  // true by parent/pom.xml override
    private boolean attachBundleArtifact = false;
    private String bundleClassifierName = "bundle";

    public Assemble(Jar jar, JavaModule module, Artifact<Path> classesArtifact, Artifact<Path> manifestArtifact) {
        this.jar = jar;
        this.module = module;
        this.classesArtifact = classesArtifact;
        this.manifestArtifact = manifestArtifact;
    }

    // TODO: tenant-cd-api, controller-server, controller-api, cloud-tenant-cd, vespa-osgi-testrunner, parent (i.e. default)
    public Assemble setUseCommonAssemblyIds(boolean useCommonAssemblyIds) {
        this.useCommonAssemblyIds = useCommonAssemblyIds;
        return this;
    }

    // TODO: bundle-plugin-test/test-bundles, controller-server, docker-api, controller-api
    public Assemble setAttachBundleArtifact(boolean attachBundleArtifact) {
        this.attachBundleArtifact = attachBundleArtifact;
        return this;
    }

    // TODO: controller-server, controller-api
    public Assemble setBundleClassifierName(String bundleClassifierName) {
        this.bundleClassifierName = bundleClassifierName;
        return this;
    }

    public AssemblerOutput declareAssemblies(Declarator declarator) {
        final String withoutSuffix;
        final String withSuffix;
        if (useCommonAssemblyIds) {
            withoutSuffix = ".jar";
            withSuffix = "-jar-with-dependencies.jar";
        } else {
            withoutSuffix = "-without-dependencies.jar";
            withSuffix = "-deploy.jar";
        }

        Artifact<Path> jarArtifact = JavaArchiver.forCreatingArchive(jar, "jar")
                .setOutputFile(Path.of("target/" + module.mavenArtifactId().artifactId() + withoutSuffix))
                .setManifestPathArtifact(manifestArtifact)
                .addDirectoryArtifact(classesArtifact)
                .declareArchive(declarator);

        Artifact<Path> withJarArtifact = JavaArchiver.forCreatingArchive(jar, "jar with dependencies")
                .setOutputFile(Path.of("target/" + module.mavenArtifactId().artifactId() + withSuffix))
                .setManifestPathArtifact(manifestArtifact)
                .addDirectoryArtifact(classesArtifact)
                // TODO: Include compile scoped jars in dependencies/, and add to Bundle-ClassPath
                .declareArchive(declarator);

        return new AssemblerOutput(jarArtifact, withJarArtifact);
    }
}
