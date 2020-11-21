package no.ion.jake.vespa.abiCheckPlugin;

import com.yahoo.abicheck.AbiCheck;
import no.ion.jake.BuildContext;

import java.nio.file.Path;

public class AbiChecker {
    public void build(BuildContext buildContext, Path jarPath) {
        var project = new ProjectImpl(buildContext.moduleContext(), jarPath);
        var log = new LogImpl(buildContext);
        var params = new com.yahoo.abicheck.AbiCheck.Params(project, log);
        AbiCheck.execute(params);
    }
}
