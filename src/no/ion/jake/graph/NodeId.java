package no.ion.jake.graph;

/**
 * Must uniquely identify build by defining {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()}.
 */
public interface NodeId {
    @Override boolean equals(Object that);
    @Override int hashCode();
    @Override String toString();
}
