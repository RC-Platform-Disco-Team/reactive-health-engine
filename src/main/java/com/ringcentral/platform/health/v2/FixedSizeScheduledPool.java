package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.HealthCheckID;
import rx.Scheduler;
import rx.internal.util.RxThreadFactory;
import rx.schedulers.Schedulers;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class FixedSizeScheduledPool implements ScheduledThreadPool {

    private final Executor executor;
    private String threadNamePrefix = "health-pool-";

    public FixedSizeScheduledPool(Integer threadPoolSize, Integer queueSize, String threadNamePrefix) {
        this(threadPoolSize, queueSize);
        this.threadNamePrefix = threadNamePrefix;
    }

    public FixedSizeScheduledPool(Integer threadPoolSize, Integer queueSize) {
        this.executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0, MILLISECONDS,
                new LinkedBlockingQueue<>(queueSize), new RxThreadFactory(threadNamePrefix));
    }

    @Override
    public Optional<Scheduler> getScheduler(HealthCheckID id) {
        return Optional.of(Schedulers.from(executor));
    }
}
