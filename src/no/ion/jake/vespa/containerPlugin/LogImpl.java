package no.ion.jake.vespa.containerPlugin;

import com.yahoo.container.plugin.api.Log;
import no.ion.jake.build.BuildContext;

public class LogImpl implements Log {
    private final BuildContext buildContext;

    public LogImpl(BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @Override
    public boolean isDebugEnabled() {
        return buildContext.log().isDebugEnabled();
    }

    @Override
    public void debug(CharSequence charSequence) {
        buildContext.log().debug(charSequence::toString);
    }

    @Override
    public void info(CharSequence charSequence) {
        buildContext.log().info(charSequence.toString());
    }

    @Override
    public void warn(CharSequence charSequence) {
        buildContext.log().warning(charSequence.toString());
    }

    @Override
    public void warn(CharSequence charSequence, Throwable cause) {
        buildContext.log().warning(charSequence.toString(), cause);
    }

    @Override
    public void warn(Throwable throwable) {
        buildContext.log().warning("", throwable);
    }
}
