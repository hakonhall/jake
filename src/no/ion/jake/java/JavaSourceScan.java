package no.ion.jake.java;

import no.ion.jake.build.Build;
import no.ion.jake.build.BuildContext;
import no.ion.jake.build.PendingPublication;

import java.nio.file.Path;
import java.util.List;

public class JavaSourceScan implements Build {

    private final String name;
    private final List<PendingPublication<Path>> pendingPublications;

    public JavaSourceScan(String name, List<PendingPublication<Path>> pendingPublications) {
        this.name = name;
        this.pendingPublications = pendingPublications;
    }

    @Override
    public String name() { return name; }

    @Override
    public void build(BuildContext buildContext) {
        pendingPublications.forEach(pendingPublication -> pendingPublication.publish(buildContext));
    }
}
