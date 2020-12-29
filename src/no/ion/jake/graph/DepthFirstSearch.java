package no.ion.jake.graph;

public class DepthFirstSearch {
    public interface Listener<U> {
        void onStartOfVisit(Vertex<U> vertex);
        void onEndOfVisit(Vertex<U> vertex);
    }

    public static <T> void traverse(Graph<T> graph, Listener<T> listener) {
        Graph<DFSNode<T>> dfsGraph = graph.transform(DFSNode::new);
        for (var root : dfsGraph.roots()) {
            if (root.get().color != DFSNode.Color.WHITE) {
                throw new IllegalStateException("assumed root cannot be a root: " + root.get().originalVertex.toString());
            }

            searchNode(root, listener);
        }
    }

    private static <V> void searchNode(Vertex<DFSNode<V>> vertex, Listener<V> listener) {
        DFSNode<V> dfsNode = vertex.get();
        switch (dfsNode.color()) {
            case WHITE:
                dfsNode.setColor(DFSNode.Color.GRAY);
                Vertex<V> originalVertex = dfsNode.originalVertex();
                listener.onStartOfVisit(originalVertex);
                for (var dependency : vertex.dependencies()) {
                    searchNode(dependency, listener);
                }
                dfsNode.setColor(DFSNode.Color.BLACK);
                listener.onEndOfVisit(originalVertex);
                break;
            case GRAY:
                throw new IllegalStateException("Loop discovered in depth-first search");
            case BLACK:
                return;
            default:
                throw new IllegalStateException("Unknown color");
        }
    }

    private static class DFSNode<W> {

        private final Vertex<W> originalVertex;

        public enum Color { WHITE, GRAY, BLACK }
        private Color color = Color.WHITE;

        public DFSNode(Vertex<W> originalVertex) {
            this.originalVertex = originalVertex;
        }

        public Vertex<W> originalVertex() { return originalVertex; }
        public Color color() { return color; }
        public void setColor(Color color) { this.color = color; }
    }
}
