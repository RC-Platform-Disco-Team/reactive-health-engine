package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.*;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.Subject;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ReactiveHealthEngineV2 implements HealthEngine {

    private final AtomicReference<HealthCheckConfig> currentHealthCheckConfig;
    private Clock clock;
    private final HealthState state;
    private final HealthCheckExecutor scheduledExecutor;
    private final Subject<HealthCheckResult, HealthCheckResultWrapper> passiveSubject;

    public ReactiveHealthEngineV2(HealthCheckConfig checkCfg, HealthEngineConfig engineCfg,
                                  Clock clock, List<HealthCheckFunction> checkFunctions) {
        this.currentHealthCheckConfig = new AtomicReference<>(checkCfg);
        this.clock = clock;
        this.state = new HealthStateV2(checkFunctions, clock.instant());
        //TODO create according to config
        ScheduledThreadPool scheduledThreadPool = new IsolatingScheduledThreadPool(checkFunctions);
        this.scheduledExecutor = new HealthCheckExecutor(engineCfg, scheduledThreadPool, clock);

        HealthCheckSplitter healthCheckSplitter = new HealthCheckSplitter(checkFunctions, state);

        //TODO extract
        Observable<HealthCheckResultWrapper> activeObservable = TickSignalObservable.createObservable(engineCfg, currentHealthCheckConfig::get, Schedulers.immediate())
                .flatMap(healthCheckSplitter::convertTickToRequest)
                .flatMap(scheduledExecutor::execute)
                .observeOn(Schedulers.newThread());
        //TODO add boolean into Config
        passiveSubject = PassiveSignalHandler.createPassiveSubject(checkFunctions, true);

        HealthResultsAnalyzer analyzer = new HealthResultsAnalyzer(state);

        activeObservable.subscribe(analyzer);
    }

    @Override
    public void updateConfig(HealthCheckConfig config) {
        currentHealthCheckConfig.set(config);
    }

    @Override
    public void sendPassiveCheckResult(HealthCheckResult result) {
        passiveSubject.onNext(result);
    }

    @Override
    public Map<HealthCheckID, LatestHealthCheckState> getAllResults() {
        return null;
    }

    @Override
    public HealthStateEnum getGlobalState() {
        return null;
    }

    @Override
    public Instant getLastChanged() {
        return null;
    }

    @Override
    public void forceCheckSync() {
    }

    @Override
    public void forceCheckAsync() {
    }

    @Override
    public void subscribeOnPassive(Observable<HealthCheckResult> passiveStream) {
        passiveStream.subscribe(passiveSubject);
    }
}
