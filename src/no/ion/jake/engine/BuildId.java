package no.ion.jake.engine;

import no.ion.jake.graph.NodeId;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class BuildId implements NodeId {
    private final String moduleName;
    private final String id;

    public BuildId(String moduleName, String id) {
        this.moduleName = requireNonNull(moduleName, "moduleName must be non-null");
        this.id = requireNonNull(id, "id must be non-null");
    }

    public String moduleName() { return moduleName; }
    public String id() { return id; }

    @Override
    public String toString() {
        return id + " in module " + moduleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildId buildId = (BuildId) o;
        return moduleName.equals(buildId.moduleName) &&
                id.equals(buildId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, id);
    }
}
