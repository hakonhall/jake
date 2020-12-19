package no.ion.jake.graph;

import java.util.List;

public class BuildOrderImpl<ID extends NodeId> implements BuildOrder<ID> {

    private final CriticalPathOrder<ID> criticalPathOrder;

    public static <ID_ extends NodeId> BuildOrder<ID_> make(List<BuildMeta<ID_>> nodes) {
        CriticalPathOrder<ID_> order = CriticalPathOrder.order(nodes);
        return new BuildOrderImpl<>(order);
    }

    public BuildOrderImpl(CriticalPathOrder<ID> criticalPathOrder) {
        this.criticalPathOrder = criticalPathOrder;
    }

    @Override
    public NextBuild<ID> calculateNextBuild() {
        return criticalPathOrder.calculateNextBuild();
    }

    @Override
    public void reportActiveBuild(ID id) {
        criticalPathOrder.reportActiveBuild(id);
    }

    @Override
    public void reportCompletedBuild(ID id) {
        criticalPathOrder.reportCompletedBuild(id);
    }

    /** If this statement compiles, the verification that BuildOrderImpl::make conforms to BuildOrderFactory is complete. */
    private static final BuildOrderFactory<? extends NodeId> apiVerifier = BuildOrderImpl::make;
}
