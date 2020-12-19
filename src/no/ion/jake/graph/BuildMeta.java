package no.ion.jake.graph;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Metadata that affects build order.
 *
 * @param <ID> Some type that the client can use to identify a build and dependencies.
 */
public interface BuildMeta<ID extends NodeId> {
    ID id();
    Duration expectedBuildDuration();
    float expectedLoad();
    Set<ID> dependencies();
    @Override String toString();
}
