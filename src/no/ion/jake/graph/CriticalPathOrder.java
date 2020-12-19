package no.ion.jake.graph;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CriticalPathOrder<ID extends NodeId> implements BuildOrder<ID> {
    private final Graph<BuildNode<ID>> pendingBuildsGraph;
    private final Map<ID, Vertex<BuildNode<ID>>> verticesById;

    public static <ID_ extends NodeId> CriticalPathOrder<ID_> order(List<BuildMeta<ID_>> builds) {
        List<BuildNode<ID_>> buildNodes = builds.stream()
                .map(BuildNode::new)
                .collect(Collectors.toList());

        var adapter = new NodeAdapter<BuildNode<ID_>, ID_>() {
            @Override public ID_ idOf(BuildNode<ID_> buildNode) { return buildNode.buildMeta().id(); }
            @Override public Set<ID_> dependenciesOf(BuildNode<ID_> buildNode) { return buildNode.buildMeta().dependencies(); }
            @Override public String toStringOfId(ID_ id) { return id.toString(); }
            @Override public String toString(BuildNode<ID_> buildNode) { return buildNode.toString(); }
        };

        Graph<BuildNode<ID_>> graph = Graph.fromAdapter(buildNodes, adapter);

        for (Vertex<BuildNode<ID_>> leafVertex : graph.leaves()) {
            calculateAndSetMinTimeSeconds(leafVertex);
        }

        return new CriticalPathOrder<>(graph);
    }

    private CriticalPathOrder(Graph<BuildNode<ID>> pendingBuildsGraph) {
        this.pendingBuildsGraph = pendingBuildsGraph;
        this.verticesById = pendingBuildsGraph.vertices().stream().collect(Collectors.toMap(
                vertex -> vertex.get().buildMeta().id(),
                vertex -> vertex));
    }

    public boolean isEmpty() { return pendingBuildsGraph.isEmpty(); }

    @Override
    public NextBuild<ID> calculateNextBuild() {
        if (pendingBuildsGraph.isEmpty()) {
            return new NextBuild<ID>() {
                @Override public boolean isDone() { return true; }
                @Override public boolean isReady() { throw new UnsupportedOperationException("all builds are done"); }
                @Override public ID getReadyBuild() { throw new UnsupportedOperationException("all builds are done"); }
            };
        }

        return pendingBuildsGraph
                .leaves()
                .stream()
                .filter(vertex -> vertex.get().state() == BuildNode.State.PENDING)
                .max(Comparator.comparing((Vertex<BuildNode<ID>> vertex) -> vertex.get().minTimeSeconds()))
                // If the cast to NextBuild<ID> is removed, this doesn't compile.  Why!?  The type created from
                // an interface ("capture of") is materially different from the interface itself!?
                .map(vertex -> (NextBuild<ID>) new NextBuild<ID>() {
                    @Override public boolean isDone() { return false; }
                    @Override public boolean isReady() { return true; }
                    @Override public ID getReadyBuild() { return vertex.get().buildMeta().id(); }
                })
                .orElseGet(() -> new NextBuild<ID>() {
                    @Override public boolean isDone() { return false; }
                    @Override public boolean isReady() { return false; }
                    @Override public ID getReadyBuild() { throw new UnsupportedOperationException("no builds are ready"); }
                });
    }

    @Override
    public void reportActiveBuild(ID id) {
        Vertex<BuildNode<ID>> vertex = getVertexFromIdOrThrow(id);
        vertex.get().setState(BuildNode.State.ACTIVE);
    }

    @Override
    public void reportCompletedBuild(ID id) {
        Vertex<BuildNode<ID>> vertex = getVertexFromIdOrThrow(id);
        pendingBuildsGraph.remove(vertex);
        verticesById.remove(id);
    }

    private static <U extends NodeId> float calculateAndSetMinTimeSeconds(Vertex<BuildNode<U>> vertex) {
        BuildNode<U> node = vertex.get();
        if (node.minTimeSeconds() >= 0f) {
            return node.minTimeSeconds();
        }

        float minTimeOfDependencies = vertex.dependencies().stream()
                .map(dependency -> calculateAndSetMinTimeSeconds(vertex))
                .min(Float::compare)
                .orElse(0f);

        return node.updateMinTimeSeconds(minTimeOfDependencies);
    }

    private Vertex<BuildNode<ID>> getVertexFromIdOrThrow(ID id) {
        var vertex = verticesById.get(id);
        if (vertex == null) {
            throw new IllegalArgumentException("no such node with ID: " + id.toString());
        }
        return vertex;
    }
}
