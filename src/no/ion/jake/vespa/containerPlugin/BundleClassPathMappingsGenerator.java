package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.bundle.GenerateBundleClassPathMappings;
import no.ion.jake.BuildContext;
import no.ion.jake.ModuleContext;
import no.ion.jake.java.ClassPathBuilder;

import java.nio.file.Path;

public class BundleClassPathMappingsGenerator {
    private final GenerateBundleClassPathMappings.Params params;
    private final ModuleContext moduleContext;

    public BundleClassPathMappingsGenerator(ModuleContext moduleContext, ClassPathBuilder classPathBuilder) {
        this.moduleContext = moduleContext;
        ProjectImpl project = new ProjectImpl(moduleContext, classPathBuilder);
        params = new GenerateBundleClassPathMappings.Params(project)
                // TODO: jdisc_core_test/test_bundles/cert-ml-dup jdisc_core_test/test_bundles/cert-ml-dup
                // jdisc_core_test/test_bundles/cert-b jdisc_core_test/test_bundles/cert-l1-dup messagebus-disc
                // metrics-proxy container-core container-search-and-docproc hosted-tenant-base provided-dependencies
                // jdisc_core config-model-fat container-messagebus
                .setBundleSymbolicName(moduleContext.name());
    }

    public void build(BuildContext buildContext) {
        GenerateBundleClassPathMappings.execute(params);
    }

    public Path outputPath() {
        return moduleContext.project().fileSystem().getPath("target/test-classes/bundle-plugin.bundle-classpath-mappings.json");
    }
}
