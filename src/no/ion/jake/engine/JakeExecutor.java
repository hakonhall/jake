package no.ion.jake.engine;

import no.ion.jake.build.Build;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JakeExecutor {
    private final float targetLoad;

    private final Object monitor = new Object();
    private final List<Thread> threads = new ArrayList<>();
    private final ArrayDeque<Build> buildQueue = new ArrayDeque<>();
    private final IdentityHashMap<Build, String> buildSet = new IdentityHashMap<>();
    private final ExecutorService threadPool;
    private final IdentityHashMap<Thread, String> activeThreads = new IdentityHashMap<>();

    private volatile float loadFactor = 1.0f;
    private final ScheduledExecutorService loadFactorExecutorService;

    public JakeExecutor(float load) {
        this.targetLoad = load;
        this.loadFactorExecutorService = Executors.newScheduledThreadPool(1);
        this.threadPool = Executors.newWorkStealingPool(Math.max(1, Math.round(load)));
        loadFactorExecutorService.scheduleAtFixedRate(this::updateLoadFactor, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void cancelAndWait() {}

    public void runAsync(Runnable runnable) {
        threadPool.submit(runnable);
    }

    private boolean canRunOneMore() {
        int running;
        synchronized (monitor) {
            running = buildSet.size();
        }

        return loadFactor * (running + 1) <= targetLoad + 0.5f;
    }

    private void updateLoadFactor() {
        int active = Thread.activeCount();

        int running;
        synchronized (monitor) {
            running = buildSet.size();
        }

        if (running > 0) {
            loadFactor = squash(0.1f, loadFactor * 0.9f + (active / (float) running) * 0.1f, 10f);
        } else {
            loadFactor = squash(0.1f, loadFactor * 0.9f, 10f);
        }
    }

    private static float squash(float lowerBound, float value, float upperBound) {
        return Math.max(lowerBound, Math.min(value, upperBound));
    }
}
