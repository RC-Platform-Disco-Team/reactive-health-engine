package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.*;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Scheduler;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static com.ringcentral.platform.health.HealthCheckSignal.Type.PERIODIC;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class HealthCheckExecutor {

    private Clock clock = Clock.systemUTC();
    private final HealthEngineConfig engineConfig;
    private final ScheduledThreadPool scheduledThreadPool;

    public HealthCheckExecutor(HealthEngineConfig engineConfig, ScheduledThreadPool scheduledThreadPool) {
        this.engineConfig = engineConfig;
        this.scheduledThreadPool = scheduledThreadPool;
    }

    public HealthCheckExecutor(HealthEngineConfig engineConfig, ScheduledThreadPool scheduledThreadPool, Clock clock) {
        this(engineConfig, scheduledThreadPool);
        this.clock = clock;
    }

    public Observable<HealthCheckResultWrapper> execute(final HealthCheckRequest request) {
        return getScheduler(request.getId()).map(scheduler ->
                Observable.just(request)
                        .map(req -> req.execute(clock, log))
                        .timeout(engineConfig.getExecutionTimeout().toMillis(), MILLISECONDS)
                        .onErrorReturn(e -> processActiveCheckError(request, e))
                        .subscribeOn(scheduler))
                .orElse(Observable.empty());
    }

    private HealthCheckResultWrapper processActiveCheckError(HealthCheckRequest request, Throwable e) {
        HealthImpactMapping impactMapping = request.getFunction().getImpactMapping();
        if (e instanceof TimeoutException) {
            Duration executionTimeout = engineConfig.getExecutionTimeout();
            log.warn("Timeout {} occurred during invocation {} healthcheck", executionTimeout.toMillis(), request.getId());
            return new HealthCheckResultWrapper(new HealthCheckResult(request.getId(), HealthStateEnum.Timeout,
                    "Execution timed out and terminated"), PERIODIC, executionTimeout, impactMapping);
        } else {
            log.error("Unexpected error during invocation of {} healthcheck", request.getId(), e);
            return new HealthCheckResultWrapper(new HealthCheckResult(request.getId(), HealthStateEnum.Critical,
                    e.getMessage()), PERIODIC, impactMapping);
        }
    }

    private Optional<Scheduler> getScheduler(HealthCheckID id) {
        return scheduledThreadPool.getScheduler(id);
    }

}
