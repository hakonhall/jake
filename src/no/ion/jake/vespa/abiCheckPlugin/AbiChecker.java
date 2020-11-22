package no.ion.jake.vespa.abiCheckPlugin;

import com.yahoo.abicheck.AbiCheck;
import no.ion.jake.BuildContext;
import no.ion.jake.build.Build;
import no.ion.jake.build.BuildResult;

import java.nio.file.Path;

public class AbiChecker implements Build {
    private Path jarPath = null;

    public AbiChecker setJarPath(Path jarPath) {
        this.jarPath = jarPath;
        return this;
    }

    @Override
    public BuildResult build(BuildContext buildContext) {
        if (jarPath == null) {
            throw new IllegalStateException("jarPath has not been set");
        }

        var project = new ProjectImpl(buildContext.moduleContext(), jarPath);
        var log = new LogImpl(buildContext);
        var params = new com.yahoo.abicheck.AbiCheck.Params(project, log);
        AbiCheck.execute(params);

        return BuildResult.of("checked abi compatibility");
    }
}
