package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.*;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.schedulers.Schedulers;

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


    public ReactiveHealthEngineV2(HealthCheckConfig checkCfg, HealthEngineConfig engineCfg,
                                  Clock clock, List<HealthCheckFunction> checkFunctions) {
        this.currentHealthCheckConfig = new AtomicReference<>(checkCfg);
        this.clock = clock;
        this.state = fillInitialState(checkFunctions);
        //TODO create according to config
        this.scheduledExecutor = new HealthCheckExecutor(engineCfg, new IsolatingScheduledThreadPool(checkFunctions), clock);

        HealthCheckSplitter healthCheckSplitter = new HealthCheckSplitter(checkFunctions, state);

        TickSignalObservable.createObservable(engineCfg, currentHealthCheckConfig::get, Schedulers.immediate())
                .flatMap(healthCheckSplitter::convertTickToRequest)
                .flatMap(scheduledExecutor::execute)
                .observeOn(Schedulers.newThread())
                .subscribe(t -> System.out.println(t.getId() + " " + t.getResult()))
        ;
    }

    private HealthState fillInitialState(List<HealthCheckFunction> checks) {
        if (checks == null) {
            return new HealthStateV1();
        }
        return new HealthStateV1(checks, clock.instant());
    }

    @Override
    public void updateConfig(HealthCheckConfig config) {
    }

    @Override
    public void sendPassiveCheckResult(HealthCheckResult result) {
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
    }
}
