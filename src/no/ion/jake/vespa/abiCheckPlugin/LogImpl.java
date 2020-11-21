package no.ion.jake.vespa.abiCheckPlugin;

import com.yahoo.abicheck.LogApi;
import no.ion.jake.BuildContext;

public class LogImpl implements LogApi {
    private final BuildContext buildContext;

    LogImpl(BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @Override
    public void debug(String s) {
        buildContext.logDebug(s);
    }

    @Override
    public void info(String s) {
        buildContext.logInfo(s);
    }

    @Override
    public void error(String s) {
        buildContext.logError(s);
    }
}
