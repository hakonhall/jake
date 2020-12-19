package no.ion.jake.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DagBuilder {
    public interface Extractor<ID, T> {
        ID idOf(T payload);
        List<ID> dependenciesOf(T payload);
        String toString(ID id);
    }

    public <ID, T> Dag<T> from(List<T> payloads, Extractor<ID, T> extractor) {
        HashMap<ID, T> payloadsById = new HashMap<>(payloads.size());
        payloads.forEach(payload -> {
            ID id = extractor.idOf(payload);
            T conflictingPayload = payloadsById.put(id, payload);
            if (conflictingPayload != null) {
                throw new IllegalArgumentException("two payloads with the same ID: " + extractor.toString(id));
            }
        });

        HashMap<ID, Set<ID>> dependenciesById = new HashMap<>(payloads.size());
        payloadsById.forEach((id, payload) -> {
            Set<ID> dependencySet = extractor.dependenciesOf(payload).stream()
                    .peek(dependencyId -> {
                        if (!payloadsById.containsKey(dependencyId)) {
                            throw new IllegalArgumentException(extractor.toString(id) + " has dependency with unknown ID: " +
                                    extractor.toString(dependencyId));
                        }
                    })
                    .collect(Collectors.toCollection(HashSet::new));
            dependenciesById.put(id, dependencySet);
        });

        HashMap<ID, Vertex<T>> vertexById = new HashMap<>(payloads.size());
        // todo
        return null;
    }
}
