package no.ion.jake.graph;

import java.util.HashSet;
import java.util.Set;

public class Vertex<T> {
    private final T payload;
    private final HashSet<Vertex<T>> dependencies = new HashSet<>();
    private final HashSet<Vertex<T>> dependees = new HashSet<>();

    public Vertex(T payload) {
        this.payload = payload;
    }

    public void addDependencyOn(Vertex<T> dependency) {
        if (!dependencies.add(dependency)) {
            throw new IllegalStateException(dependency.toString() + " is already a dependency of " + toString());
        }
        if (!dependency.dependees.add(this)) {
            throw new IllegalStateException(toString() + " is already a dependee of " + dependency.toString());
        }
    }

    public void removeDependencyOn(Vertex<T> dependency) {
        if (!dependencies.remove(dependency)) {
            throw new IllegalStateException(dependency.toString() + " is not a dependency of " + this.toString());
        }
        if (!dependency.dependees.remove(this)) {
            throw new IllegalStateException(toString() + " is not a dependee of " + dependency.toString());
        }
    }

    public T get() { return payload; }
    public Set<Vertex<T>> dependencies() { return dependencies; }  // WARNING: returns mutable set
    public Set<Vertex<T>> dependees() { return dependees; }  // WARNING: returns mutable set

    @Override
    public String toString() { return "Vertex{" + (payload == null ? "" : payload.toString()) + "}"; }
}
