package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.api.Log;
import no.ion.jake.BuildContext;

public class LogImpl implements Log {
    private final BuildContext buildContext;

    public LogImpl(BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @Override
    public boolean isDebugEnabled() {
        return buildContext.isDebugEnabled();
    }

    @Override
    public void debug(CharSequence charSequence) {
        buildContext.logDebug(charSequence::toString);
    }

    @Override
    public void info(CharSequence charSequence) {
        buildContext.logInfo(charSequence.toString());
    }

    @Override
    public void warn(CharSequence charSequence) {
        buildContext.logWarning(charSequence.toString());
    }

    @Override
    public void warn(CharSequence charSequence, Throwable cause) {
        buildContext.logWarning(charSequence.toString(), cause);
    }

    @Override
    public void warn(Throwable throwable) {
        buildContext.logWarning("", throwable);
    }
}
