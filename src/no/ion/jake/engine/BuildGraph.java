package no.ion.jake.engine;

import com.sun.management.ThreadMXBean;
import no.ion.jake.LogSink;
import no.ion.jake.graph.BuildOrder;
import no.ion.jake.util.SetUtil;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.util.function.Function.identity;

public class BuildGraph implements AutoCloseable {
    private final JakeExecutor jakeExecutor;
    private final ScheduledExecutorService loadUpdaterService;
    private final ThreadMXBean threadMXBean;
    private final BuildOrder<BuildId> buildOrder;
    private final ArtifactRegistry artifactRegistry;
    private final LogSink logSink;
    private final Map<BuildId, BuildInfo> builds;

    private final float targetLoad;
    private final AtomicLong loadx1000 = new AtomicLong(0L);
    private final AtomicLong artificialLoadx1000 = new AtomicLong(0L);
    private final AtomicLong pendingArtificialLoadx1000 = new AtomicLong(0L);

    private final Object monitor = new Object();

    private static final int loadUpdateIntervalInMillis = 100;
    private volatile long lastSumCpuTimeNanos = 0;

    public BuildGraph(JakeExecutor jakeExecutor, float targetLoad, BuildOrder<BuildId> buildOrder,
                      Collection<BuildInfo> builds, ArtifactRegistry artifactRegistry, LogSink logSink) {
        this.jakeExecutor = jakeExecutor;
        this.targetLoad = targetLoad;
        this.buildOrder = buildOrder;
        this.artifactRegistry = artifactRegistry;
        this.logSink = logSink;
        this.builds = builds.stream().collect(Collectors.toMap(BuildInfo::id, identity()));

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

    public void runSync() {
        Deque<ResultInfo> results = new ArrayDeque<>();

        synchronized (monitor) {
            while (true) {
                BuildOrder.NextBuild<BuildId> nextBuild = buildOrder.calculateNextBuild();
                if (nextBuild.isDone()) {
                    return;
                }

                if (nextBuild.isReady()) {
                    if (spawnBuild(nextBuild.getReadyBuild(), results)) {
                        continue;
                    }
                }

                if (!results.isEmpty()) {
                    handleResultInfo(results.pollFirst());
                    continue;
                }

                // getting here also if load is too high.  Should sleep until the load have had a chance to lower.
                try { monitor.wait(10); } catch (InterruptedException ignore) { }
            }
        }
    }

    private static class ResultInfo {
        private final BuildInfo buildInfo;
        private final BuildResult result;

        public ResultInfo(BuildInfo buildInfo, BuildResult result) {
            this.buildInfo = buildInfo;
            this.result = result;
        }
    }

    private boolean spawnBuild(BuildId buildId, Deque<ResultInfo> results) {
        final BuildInfo buildInfo = builds.get(buildId);
        if (buildInfo == null) {
            throw new IllegalStateException("build ID does not exist: " + buildId.toString());
        }

        if (!loadAllowsAnotherBuild(buildInfo)) {
            return false;
        }

        buildOrder.reportActiveBuild(buildId);

        // TODO: Add something other than 1, e.g. expectedLoad + slack.
        final long loadx1000 = 1000L;
        artificialLoadx1000.addAndGet(loadx1000);

        jakeExecutor.runAsync(() -> {
            pendingArtificialLoadx1000.addAndGet(-loadx1000);

            SingleBuildDriver driver = new SingleBuildDriver(logSink);
            BuildResult result = driver.runSync(artifactRegistry, buildInfo);

            synchronized (monitor) {
                buildOrder.reportCompletedBuild(buildId);
                results.addLast(new ResultInfo(buildInfo, result));
                monitor.notify();
            }
        });

        return true;
    }

    private void handleResultInfo(ResultInfo resultInfo) {
        BuildResult result = resultInfo.result;
        BuildInfo completedBuild = resultInfo.buildInfo;

        result.getRuntimeException().ifPresent(e -> {
            throw e;
        });
        result.getError().ifPresent(e -> {
            throw e;
        });

        Set<ArtifactId> declaredArtifacts = completedBuild.production();
        Set<ArtifactId> publishedArtifacts = result.publishedArtifacts();

        Set<ArtifactId> unpublishedArtifacts = SetUtil.difference(declaredArtifacts, publishedArtifacts);
        if (!unpublishedArtifacts.isEmpty()) {
            throw new BadBuildException(completedBuild.build(), "failed to publish: " +
                    unpublishedArtifacts.stream().map(ArtifactId::artifactName).collect(Collectors.joining(", ")));
        }

        Set<ArtifactId> unknownArtifacts = SetUtil.difference(publishedArtifacts, declaredArtifacts);
        if (!unknownArtifacts.isEmpty()) {
            // this is impossible at the time this comment was made
            throw new BadBuildException(completedBuild.build(), "published extraneous artifacts: " +
                    unknownArtifacts.stream().map(ArtifactId::artifactName).collect(Collectors.joining(", ")));
        }
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
    }

    private boolean loadAllowsAnotherBuild(BuildInfo buildInfo) {
        float currentTargetLoad = targetLoad > 0 ? targetLoad : -targetLoad * Runtime.getRuntime().availableProcessors();

        // If adding another builds gets us within +- 0.5 of target load, or lower.
        // The artificial load is artificial load is incremented preemptively, in case true is returned.
        float newLoad = (loadx1000.get() + artificialLoadx1000.get()) / 1000.0f + 1f;
        return newLoad < currentTargetLoad + 0.5f;
    }

    private void updateLoad() {
        long previousSumCpuTimes = lastSumCpuTimeNanos;

        long[] threadIds = threadMXBean.getAllThreadIds();
        long[] threadCpuTimesNanos = threadMXBean.getThreadCpuTime(threadIds);
        long sumCpuTimeNanos = LongStream.of(threadCpuTimesNanos).sum();
        long nanos = Math.max(0L, sumCpuTimeNanos - previousSumCpuTimes);

        float newLoad = nanos / (1000_000f * loadUpdateIntervalInMillis);
        loadx1000.set((long) (newLoad * 1000L));
        lastSumCpuTimeNanos = sumCpuTimeNanos;

        artificialLoadx1000.addAndGet(pendingArtificialLoadx1000.getAndSet(0L));
    }
}
