package no.ion.jake.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A directed acyclic graph.
 *
 * @param <T> type of payload
 */
public class Dag<T> {
    private final HashSet<Vertex<T>> vertices;
    private final HashSet<Vertex<T>> roots;
    private final HashSet<Vertex<T>> leaves;

    public Dag() { this(new HashSet<>(), new HashSet<>(), new HashSet<>()); }

    private Dag(HashSet<Vertex<T>> vertices, HashSet<Vertex<T>> roots, HashSet<Vertex<T>> leaves) {
        this.vertices = vertices;
        this.roots = roots;
        this.leaves = leaves;
    }

    public boolean isEmpty() { return vertices.isEmpty(); }
    public int numVertices() { return vertices.size(); }
    public Set<Vertex<T>> vertices() { return vertices; }
    public Set<Vertex<T>> roots() { return roots; }
    public Set<Vertex<T>> leaves() { return leaves; }

    public Vertex<T> addVertex(T payload, List<Vertex<T>> dependencies) {
        Vertex<T> vertex = new Vertex<>(payload);
        vertices.add(vertex);
        roots.add(vertex);
        if (dependencies.isEmpty()) {
            leaves.add(vertex);
        } else {
            dependencies.forEach(dependency -> {
                vertex.addDependencyOn(dependency);
                roots.remove(dependency);
            });
        }

        return vertex;
    }

    public void removeVertex(Vertex<T> vertex) {
        if (!vertices.remove(vertex)) {
            throw new IllegalStateException(vertex.toString() + " is not a vertex of " + toString());
        }
        roots.remove(vertex);
        leaves.remove(vertex);

        for (Vertex<T> dependee : vertex.dependees()) {
            dependee.removeDependencyOn(vertex);
        }

        for (Vertex<T> dependency : vertex.dependencies()) {
            vertex.removeDependencyOn(dependency);
        }
    }

    /**
     * Returns a new DAG identical to this, with payload of type {@code U} instead of {@code T}, and with each
     * payload created by {@code transformer} and based on the old vertex.
     */
    public <U> Dag<U> transform(NodeTransformer<T, U> nodeTransformer) {
        HashSet<Vertex<U>> newVertices = new HashSet<>(numVertices());
        HashSet<Vertex<U>> newRoots = new HashSet<>(numVertices());
        HashSet<Vertex<U>> newLeaves = new HashSet<>(numVertices());

        // Make a 1:1 map from old to new vertices
        Map<Vertex<T>, Vertex<U>> newByOld = new HashMap<>(vertices.size());
        for (Vertex<T> oldVertex : vertices) {
            U newInstance = nodeTransformer.transform(oldVertex);
            Vertex<U> newVertex = new Vertex<>(newInstance);
            newByOld.put(oldVertex, newVertex);
        }

        newByOld.forEach((oldVertex, newVertex) -> {
            newVertices.add(newVertex);

            // Fix dependencies
            for (Vertex<T> oldDependency : oldVertex.dependencies()) {
                Vertex<U> newDependency = newByOld.get(oldDependency);
                newVertex.addDependencyOn(newDependency);
            }
        });

        roots.forEach(oldVertex -> newRoots.add(newByOld.get(oldVertex)));
        leaves.forEach(oldVertex -> newLeaves.add(newByOld.get(oldVertex)));

        return new Dag<>(newVertices, newRoots, newLeaves);
    }

    @Override
    public String toString() {
        return "Graph{" + vertices.size() + " nodes}";
    }
}
