package no.ion.jake.engine;

import com.sun.management.ThreadMXBean;
import no.ion.jake.LogSink;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.util.function.Function.identity;

public class BuildGraph2 implements AutoCloseable {
    private final JakeExecutor jakeExecutor;
    private final ScheduledExecutorService loadUpdaterService;
    private final ThreadMXBean threadMXBean;
    private final LogSink logSink;
    private final Map<BuildNodeId, BuildNode> builds;

    private final float targetLoad;
    private final AtomicLong loadx1000 = new AtomicLong(0L);
    private final AtomicInteger artificialLoad = new AtomicInteger(1);

    private final Object monitor = new Object();
    private final Set<BuildNodeId> pendingBuilds;
    private final Set<BuildNodeId> activeBuilds = new HashSet<>();
    private final Set<BuildNodeId> completedBuilds;

    private Runnable onDone = null;

    private final Object runMoreBuildsMonitor = new Object();
    private static final int loadUpdateIntervalInMillis = 100;
    private volatile long lastSumCpuTimeNanos = 0;

    public BuildGraph2(JakeExecutor jakeExecutor, List<BuildNode> builds, float targetLoad, LogSink logSink) {
        this.jakeExecutor = jakeExecutor;
        this.logSink = logSink;
        this.targetLoad = targetLoad;
        this.builds = builds.stream().collect(Collectors.toMap(BuildNode::id, identity()));
        this.pendingBuilds = builds.stream().map(BuildNode::id).collect(Collectors.toCollection(HashSet::new));
        this.completedBuilds = new HashSet<>(builds.size());

        java.lang.management.ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if (!(threadMXBean instanceof ThreadMXBean)) {
            // add other JVMs when needed...
            throw new IllegalStateException("implementation requires Sun/Oracle based JVM");
        }
        this.threadMXBean = (ThreadMXBean) threadMXBean;
        if (!threadMXBean.isThreadCpuTimeSupported()) {
            throw new IllegalStateException("implementation requires JVM supports thread CPU time");
        }
        if (!threadMXBean.isThreadCpuTimeEnabled()) {
            threadMXBean.setThreadCpuTimeEnabled(true);
        }

        this.loadUpdaterService = Executors.newScheduledThreadPool(1);
        loadUpdaterService.scheduleAtFixedRate(this::updateLoad, 0, loadUpdateIntervalInMillis, TimeUnit.MILLISECONDS);
    }

    public void start(Runnable onDone) {
        this.onDone = onDone;
        runMoreBuilds();
    }

    @Override
    public void close() {
        loadUpdaterService.shutdown();
        for (;;) {
            try {
                if (loadUpdaterService.awaitTermination(1, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException ignored) {
                logSink.log(Level.WARNING, "was interrupted during shutdown, ignoring", null);
            }

            // loop around
        }

        throw new IllegalStateException("TODO: cleanup active and pending builds");
    }

    private void runMoreBuilds() {
        // Optimization
        synchronized (monitor) {
            if (pendingBuilds.isEmpty()) {
                return;
            }
        }

        synchronized (runMoreBuildsMonitor) {
            while (loadAllowsAnotherBuild()) {
                synchronized (monitor) {
                    Optional<BuildNode> nodeToBuild = findNodeToBuild();
                    if (nodeToBuild.isEmpty()) {
                        return;
                    }
                }


                // preempt updateLoad: add 1 to avoid multiple runMoreBuilds to schedule many builds before the load
                // is updated to reflect the newly added builds.  This must be decremented once the build starts.
                // TODO: Add decrement elsewhere.
                artificialLoad.addAndGet(1);
            }
        }
    }

    /**
     *
     */
    private Optional<BuildNode> findNodeToBuild() {
        // Algorithm:
        //  - calculate the critical path among the pending and active builds, assuming normal (or assumed) progression.
        //  - If the initial build is pending, that is the one to build (and it MUST NOT have any pending or active
        //    dependencies, by construction).
        //  - Otherwise, it must be active.  Say it has T time remaining, assuming normal progression.  Assume that
        //    time passes, and pretend the active build completes, along with any other active builds that would
        //    complete within T with normal progression.
        //  - the set of candidates starts out as the set of pending builds, that do not depend on any pending or
        //    active builds.
        //  - remove all candidates that depends on a pending or active build.
        //  - .

        // For all pending builds, each has a number of dependencies.  Ignore all pending builds that depends
        // on a build that is active, recursively.  Assume normal progression
        // of all dependencies of activate builds.  Calculate the critical path picking pending builds only, and pick that to build.
        // if that build is already active, assume it will be built and remove it from.

        // todo
        return null;
    }

    private boolean loadAllowsAnotherBuild() {
        float currentTargetLoad = targetLoad > 0 ? targetLoad : -targetLoad * Runtime.getRuntime().availableProcessors();

        // If adding another builds gets us within +- 0.5 of target load, or lower.
        // The artificial load is artificial load is incremented preemptively, in case true is returned.
        boolean allowed = loadx1000.get() / 1000.0 + artificialLoad.get() + 1 < currentTargetLoad + 0.5f;
        return allowed;
    }

    private void updateLoad() {
        long previousSumCpuTimes = lastSumCpuTimeNanos;

        long[] threadIds = threadMXBean.getAllThreadIds();
        long[] threadCpuTimesNanos = threadMXBean.getThreadCpuTime(threadIds);
        long sumCpuTimeNanos = LongStream.of(threadCpuTimesNanos).sum();
        long nanos = Math.max(0L, sumCpuTimeNanos - previousSumCpuTimes);

        loadx1000.addAndGet(nanos / loadUpdateIntervalInMillis);
        lastSumCpuTimeNanos = nanos;
    }
}
