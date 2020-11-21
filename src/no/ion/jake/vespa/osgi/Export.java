package no.ion.jake.vespa.osgi;

import java.util.List;

public class Export {
    private final List<String> packageNames;
    private final List<Parameter> parameters;

    public Export(List<String> packageNames, List<Parameter> parameters) {
        this.packageNames = packageNames;
        this.parameters = parameters;
    }

    public List<String> getPackageNames() {
        return packageNames;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }
}
