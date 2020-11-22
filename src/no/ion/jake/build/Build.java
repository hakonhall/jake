package no.ion.jake.build;

import no.ion.jake.BuildContext;

public interface Build {
    BuildResult build(BuildContext buildContext);
}
