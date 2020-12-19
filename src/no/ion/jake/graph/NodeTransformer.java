package no.ion.jake.graph;

@FunctionalInterface
public interface NodeTransformer<T, U> {
    /** Returns the payload for the vertex to be replaced by the given old/current vertex. */
    U transform(Vertex<T> oldVertex);
}
