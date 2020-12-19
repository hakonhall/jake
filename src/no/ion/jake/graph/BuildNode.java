package no.ion.jake.graph;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

public class BuildNode<ID extends NodeId> {
    private static final Map<State, EnumSet<State>> LEGAL_STATE_TRANSITIONS = Map.of(
            State.PENDING, EnumSet.of(State.ACTIVE));

    private final BuildMeta<ID> buildMeta;

    private float minTimeSeconds = -1;
    private State state = State.PENDING;

    public BuildNode(BuildMeta<ID> buildMeta) { this.buildMeta = buildMeta; }

    public BuildMeta<ID> buildMeta() { return buildMeta; }
    public float minTimeSeconds() { return minTimeSeconds; }

    public enum State {PENDING, ACTIVE}
    public State state() { return state; }

    public void setState(State newState) {
        Objects.requireNonNull(newState);
        if (!LEGAL_STATE_TRANSITIONS.get(state).contains(newState)) {
            throw new IllegalStateException("illegal state transition: " + state + " -> " + newState);
        }
        state = newState;
    }

    public float updateMinTimeSeconds(float minTimeOfDependencies) {
        // Use 0.001f to enforce strictly increasing minTimeSeconds in the critical path chain.
        minTimeSeconds = Math.max(0.001f, minTimeOfDependencies) + buildMeta.expectedBuildDuration().toMillis() / 1000f;

        return minTimeSeconds;
    }

    @Override
    public String toString() {
        return "BuildNode{" +
                "buildMeta=" + buildMeta +
                ", minTimeSeconds=" + minTimeSeconds +
                ", state=" + state +
                '}';
    }
}
