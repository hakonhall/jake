package no.ion.jake.engine;

import no.ion.jake.graph.NodeId;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class BuildId implements NodeId {
    private final String namespace;
    private final String id;

    public BuildId(String namespace, String id) {
        this.namespace = requireNonNull(namespace, "moduleName must be non-null");
        this.id = requireNonNull(id, "id must be non-null");
    }

    public String namespace() { return namespace; }
    public String id() { return id; }

    @Override
    public String toString() {
        return id + " in module " + namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildId buildId = (BuildId) o;
        return namespace.equals(buildId.namespace) &&
                id.equals(buildId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, id);
    }
}
