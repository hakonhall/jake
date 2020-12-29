package no.ion.jake.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Graph<T> {
    private final HashSet<Vertex<T>> vertices;

    public static <ID extends NodeId, U> Graph<U> fromAdapter(List<U> nodes, NodeAdapter<U, ID> adapter) {
        HashMap<ID, Vertex<U>> vertexById = new HashMap<>();
        nodes.forEach(node -> {
            ID id = adapter.idOf(node);
            Vertex<U> vertex = new Vertex<>(node);
            Vertex<U> previousVertex = vertexById.put(id, vertex);
            if (previousVertex != null) {
                throw new IllegalArgumentException("node " + adapter.toString(previousVertex.get()) + " and node " +
                        adapter.toString(vertex.get()) + " have equal ID: " + adapter.toStringOfId(id));
            }
        });

        vertexById.values().forEach(vertex -> {
            U node = vertex.get();
            adapter.dependenciesOf(node).forEach(dependencyId -> {
                Vertex<U> dependency = vertexById.get(dependencyId);
                if (dependency == null) {
                    throw new IllegalArgumentException("node " + adapter.toString(node) + " has a dependency on ID " +
                            adapter.toStringOfId(dependencyId) + " which is not in the node set");
                }

                vertex.addDependencyOn(dependency);
            });
        });

        return new Graph<>(vertexById.values());
    }

    private Graph(Collection<Vertex<T>> vertices) {
        this.vertices = new HashSet<>(vertices);
    }

    public void remove(Vertex<T> vertex) {
        if (!vertices.remove(vertex)) {
            throw new IllegalArgumentException("vertex not a member of this graph: " + vertex.toString());
        }

        Set<Vertex<T>> dependees = Set.copyOf(vertex.dependees());
        dependees.forEach(dependee -> dependee.removeDependencyOn(vertex));

        Set<Vertex<T>> dependencies = Set.copyOf(vertex.dependencies());
        dependencies.forEach(vertex::removeDependencyOn);
    }

    public boolean isEmpty() { return vertices.isEmpty(); }
    public int numVertices() { return vertices.size(); }

    /** WARNING: Returns mutable set. */
    public Set<Vertex<T>> vertices() { return vertices; }

    /** WARNING: Returns mutable set. */
    public Set<Vertex<T>> roots() {
        return vertices.stream().filter(vertex -> vertex.dependees().isEmpty()).collect(Collectors.toSet());
    }

    /** WARNING: Returns mutable set. */
    public Set<Vertex<T>> leaves() {
        return vertices.stream().filter(vertex -> vertex.dependencies().isEmpty()).collect(Collectors.toSet());
    }

    /** Returns a graph identical to this, except with own vertices.  The payloads are identical. */
    public Graph<T> duplicate() {
        return new Graph<>(vertices);
    }

    /** Returns a graph with vertices created by transformer of the old vertices, and dependencies identical to this. */
    public <U> Graph<U> transform(NodeTransformer<T, U> transformer) {
        HashMap<Vertex<T>, Vertex<U>> newByOld = new HashMap<>(vertices.size());

        vertices.forEach(oldVertex -> {
            U newInstance = transformer.transform(oldVertex);
            Vertex<U> newVertex = new Vertex<>(newInstance);
            newByOld.put(oldVertex, newVertex);
        });

        vertices.forEach(oldVertex -> {
            Vertex<U> newVertex = newByOld.get(oldVertex);

            for (Vertex<T> oldDependency : oldVertex.dependencies()) {
                Vertex<U> newDependency = newByOld.get(oldDependency);

                newVertex.addDependencyOn(newDependency);
            }
        });

        return new Graph<U>(newByOld.values());
    }
}
