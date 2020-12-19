package no.ion.jake.graph;

import java.util.List;
import java.util.Set;

/**
 * If we got a list of instances of type {@code T}, and we want that to be the payload of the vertices of a DAG,
 * then this interface defines how the dependencies can be extracted from the payload.
 *
 * Instances of type {@code T} is supposed to be put in a graph.  A NodeAdapter defines how such instances relate
 * in a graph.  From T a type specific to the graph, type ID, can be extracted that can be used to uniquely
 * identify a node (equals and hashCode), as well as it's string representation (toString).
 */
public interface NodeAdapter<T, ID> {
    /** Returns an object uniquely identifying the node, and must define equals(), hashCode(), and toString(). */
    ID idOf(T node);

    Set<ID> dependenciesOf(T node);

    default String toString(T node) { return idOf(node).toString(); }

    default String toStringOfId(ID id) { return id.toString(); }
}
