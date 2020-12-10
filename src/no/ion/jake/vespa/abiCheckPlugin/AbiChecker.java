package no.ion.jake.vespa.abiCheckPlugin;

import com.yahoo.abicheck.AbiCheck;
import com.yahoo.abicheck.LogApi;
import com.yahoo.abicheck.ProjectApi;
import no.ion.jake.build.Artifact;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.build.Declarator;
import no.ion.jake.build.JavaModule;
import no.ion.jake.build.ModuleContext;

import java.io.File;
import java.nio.file.Path;

public class AbiChecker {
    public static Artifact<Void> check(Artifact<Path> jarPathArtifact, ModuleContext moduleContext, JavaModule module,
                                       Declarator declarator) {
        try (var declaration = declarator.declareNewBuild()) {
            declaration.dependsOn(jarPathArtifact);

            Artifact<Void> checkArtifact = declaration.producesArtifact(Void.class, "abi check");

            declaration.forBuild(new Build() {
                @Override public String name() { return module.moduleName(); }

                @Override
                public void build(BuildContext buildContext) {
                    var projectAdapter = new ProjectApi() {
                        @Override public File jarFile() { return moduleContext.resolve(jarPathArtifact.detail()).toFile(); }
                        @Override public File getBasedir() { return moduleContext.resolve(".").toFile(); }
                    };

                    var logAdapter = new LogApi() {
                        @Override public void debug(String message) { buildContext.log().debug(message); }
                        @Override public void info(String message) { buildContext.log().info(message); }
                        @Override public void error(String message) { buildContext.log().error(message); }
                    };

                    var params = new AbiCheck.Params(projectAdapter, logAdapter);
                    AbiCheck.execute(params);

                    buildContext.newPublicationOf(checkArtifact)
                            .logWithDuration("checked abi compatibility")
                            .publish(null);
                }
            });

            return checkArtifact;
        }
    }
}
