package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.*;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("FieldCanBeLocal")
@Slf4j
public class ReactiveHealthEngineV2 implements HealthEngine {

    private final AtomicReference<HealthCheckConfig> currentHealthCheckConfig;
    private Clock clock = Clock.systemUTC();
    private final HealthState state;
    private final HealthCheckExecutor scheduledExecutor;
    private final HealthCheckExecutor forcedExecutor;
    private final Subject<HealthCheckResult, HealthCheckResultWrapper> passiveSubject;
    private final Subject<HealthCheckResultWrapper, HealthCheckResultWrapper> forceSubject;

    public ReactiveHealthEngineV2(HealthCheckConfig checkCfg, HealthEngineConfig engineCfg,
                                  Clock clock, List<HealthCheckFunction> checkFunctions) {
        this.currentHealthCheckConfig = new AtomicReference<>(checkCfg);
        this.clock = clock;
        this.state = new HealthStateV2(checkFunctions, clock.instant());
        //TODO create according to config
        ScheduledThreadPool scheduledThreadPool = new IsolatingScheduledThreadPool(checkFunctions);
        this.scheduledExecutor = new HealthCheckExecutor(engineCfg, scheduledThreadPool, clock);

        ScheduledThreadPool forcedThreadPool = new IsolatingScheduledThreadPool(checkFunctions);
        this.forcedExecutor = new HealthCheckExecutor(engineCfg, forcedThreadPool, clock);
        forceSubject = PublishSubject.<HealthCheckResultWrapper>create().toSerialized();

        HealthCheckSplitter healthCheckSplitter = new HealthCheckSplitter(checkFunctions, state);

        //TODO extract
        Observable<HealthCheckResultWrapper> activeObservable = TickSignalObservable.create(engineCfg, currentHealthCheckConfig::get, Schedulers.immediate())
                .flatMap(healthCheckSplitter::convertTickToRequest)
                .flatMap(scheduledExecutor::execute)
                .observeOn(Schedulers.newThread());
        //TODO add boolean into Config
        passiveSubject = PassiveSignalHandler.createPassiveSubject(checkFunctions, true);

        Subscriber<HealthCheckSignal> analyzer = HealthResultsAnalyzer.create(state);

        Observable<HealthCheckResultWrapper> resultingObservable = Observable.merge(passiveSubject, activeObservable, forceSubject);

        resultingObservable.subscribe(analyzer);

        TickSignalObservable.once(currentHealthCheckConfig::get).flatMap(healthCheckSplitter::convertTickToRequest)
                .flatMap(forcedExecutor::execute).toBlocking().subscribe(forceSubject::onNext, e -> {throw new ForceHealthCheckFailedException("", e);});
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
        return state.getDetails();
    }

    @Override
    public HealthStateEnum getGlobalState() {
        return state.getGlobalState();
    }

    @Override
    public Instant getLastChanged() {
        return state.getLastChanged();
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
