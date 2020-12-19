package no.ion.jake.graph;

import java.util.List;

@FunctionalInterface
public interface BuildOrderFactory<ID extends NodeId> {
    BuildOrder<ID> make(List<BuildMeta<ID>> nodes);
}
