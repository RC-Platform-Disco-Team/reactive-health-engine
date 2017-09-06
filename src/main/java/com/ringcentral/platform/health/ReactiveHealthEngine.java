package com.ringcentral.platform.health;

import com.ringcentral.platform.rx.DistinctResultsOperator;
import com.ringcentral.platform.rx.SafeMapOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Actions;
import rx.internal.util.ActionSubscriber;
import rx.observers.SerializedSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static com.ringcentral.platform.health.HealthCheckSignal.Type.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ReactiveHealthEngine implements HealthEngine {

    private HealthCheckConfig currentHealthCheckConfig;

    private final Logger log;
    private final HealthStateV1 state;
    private final Executor scheduledExecutor;
    private final Executor forcedExecutor;
    private final Duration executionTimeout;
    private final Duration expirationPeriodDelta;
    private final Clock clock;
    private final Map<HealthCheckID, HealthCheckFunction> checkFunctionsMap;
    private final Subject<HealthCheckResult, HealthCheckResult> passiveSubject = PublishSubject.<HealthCheckResult>create().toSerialized();
    private final Subject<HealthCheckResultWrapper, HealthCheckResultWrapper> forceSubject = PublishSubject.<HealthCheckResultWrapper>create().toSerialized();
    private final Subject<HealthCheckSignal, HealthCheckSignal> managementSubject = PublishSubject.<HealthCheckSignal>create().toSerialized();
    private final Subject<HealthCheckRequest, HealthCheckRequest> rescheduleSubject = PublishSubject.<HealthCheckRequest>create().toSerialized();

    public ReactiveHealthEngine(HealthCheckConfig healthCheckConfig, HealthEngineConfig healthEngineConfig,
                                Clock clock, HealthCheckFunction... checkFunctions) {
        this.log = LoggerFactory.getLogger(healthEngineConfig.getLoggerName());
        this.clock = clock;

        this.currentHealthCheckConfig = healthCheckConfig;

        this.expirationPeriodDelta = healthEngineConfig.getExpirationPeriodDelta();
        this.scheduledExecutor = new ThreadPoolExecutor(healthEngineConfig.getScheduledThreadPoolSize(), healthEngineConfig.getScheduledThreadPoolSize(),
                0, MILLISECONDS, new LinkedBlockingQueue<>(healthEngineConfig.getScheduledQueueSize()));
        this.forcedExecutor = new ThreadPoolExecutor(healthEngineConfig.getForcedThreadPoolSize(), healthEngineConfig.getForcedThreadPoolSize(),
                0, MILLISECONDS, new LinkedBlockingQueue<>(healthEngineConfig.getForcedQueueSize()));
        this.executionTimeout = healthEngineConfig.getExecutionTimeout();
        this.checkFunctionsMap = convertCheckersToMap(checkFunctions, HealthCheckFunction::getId, t -> t);
        this.state = fillInitialHealthState(checkFunctions, healthCheckConfig);

        scheduleFirstChecks(healthCheckConfig, healthEngineConfig);
    }

    @Override
    public void subscribeOnPassive(Observable<HealthCheckResult> passiveStream) {
        log.debug("Subscribe HealthEngine on aggregated stream");
        passiveStream.subscribe(passiveSubject);
    }

    private void scheduleFirstChecks(HealthCheckConfig healthCheckConfig, HealthEngineConfig healthEngineConfig) {
        Observable<HealthCheckRequest> initialObservable = createInitialSchedulingObservable(healthCheckConfig, healthEngineConfig, checkFunctionsMap.values());
        Observable<HealthCheckSignal> expirationSignalObservable = createExpirationSignalObservable(healthEngineConfig); // sends repeated expiration signals
        Observable<HealthCheckResultWrapper> inputObservable = Observable.merge(initialObservable, rescheduleSubject)
                .flatMap(this::parallelActiveCheckExecution);
        Observable<HealthCheckResultWrapper> passiveObservable = createPassiveObservable();

        Observable<HealthCheckResultWrapper> filteredCheckResultObservable = Observable.merge(inputObservable, forceSubject, passiveObservable)
                .groupBy(HealthCheckResultWrapper::getId)
                .flatMap(go -> go.lift(new DistinctResultsOperator())); // this filters non-changing signals

        SerializedSubscriber<HealthCheckSignal> healthAnalyzerObserver = createHealthAnalyzerObserver();
        Observable.merge(expirationSignalObservable, managementSubject, filteredCheckResultObservable)
                .observeOn(Schedulers.newThread()) // execute state-changing logic in single thread
                .subscribe(healthAnalyzerObserver);
    }


    private SerializedSubscriber<HealthCheckSignal> createHealthAnalyzerObserver() {
        return new SerializedSubscriber<>(new ActionSubscriber<>(
                this::processHealthCheckSignal,
                e -> log.warn(e.getMessage(), e),
                Actions.empty()
        ));
    }

    private Observable<HealthCheckSignal> createExpirationSignalObservable(HealthEngineConfig healthEngineConfig) {
        return Observable.interval(healthEngineConfig.getExpirationSignalPeriod().getSeconds(), SECONDS)
                .map(l -> HealthCheckSignal.expiration());
    }

    private Observable<HealthCheckRequest> createInitialSchedulingObservable(HealthCheckConfig healthCheckConfig, HealthEngineConfig healthEngineConfig, Collection<HealthCheckFunction> checks) {
        LongStream initialDelays = healthEngineConfig.initialDelaysInSeconds(checks.size());
        return Observable.from(checks).map(f -> createRequest(f, PERIODIC, healthCheckConfig))
                .zipWith(initialDelays::iterator, (ch, delay) -> Observable.just(ch).delay(delay, SECONDS))
                .flatMap(t -> t);
    }

    private HealthStateV1 fillInitialHealthState(HealthCheckFunction[] checkers, HealthCheckConfig healthCheckConfig) {
        if (checkers != null) {
            Instant instant = clock.instant();
            return new HealthStateV1(Arrays.asList(checkers), instant);
        } else {
            return new HealthStateV1();
        }
    }

    private <T, U> Map<T, U> convertCheckersToMap(HealthCheckFunction[] checkers, Function<HealthCheckFunction, T> keyF, Function<HealthCheckFunction, U> valueF) {
        return checkers == null ? Collections.emptyMap() : Stream.of(checkers).collect(Collectors.toMap(keyF, valueF));
    }

    private HealthCheckRequest createRequest(HealthCheckFunction function, HealthCheckSignal.Type type, HealthCheckConfig config) {
        return new HealthCheckRequest(function, type, config.isDisabled(function.getId()), config.getSlowTimeout(function.getId()));
    }

    private Observable<HealthCheckResultWrapper> createPassiveObservable() {
        return passiveSubject
                .filter(result -> {     // filter non-existing passive signals
                    if (!checkFunctionsMap.containsKey(result.getId())) {
                        log.warn("received passive event {} for non-existing check {}", result.getState(), result.getId());
                        return false;
                    }
                    return true;
                })
                .lift(new SafeMapOperator<>(false,
                        result -> {
                            log.debug("received passive event {} for {}", result.getState(), result.getId());
                            HealthImpactMapping impactMapping = checkFunctionsMap.get(result.getId()).getImpactMapping();
                            return new HealthCheckResultWrapper(result, PASSIVE, impactMapping);
                        },
                        e -> log.warn(e.getMessage(), e)));
    }

    private Observable<HealthCheckResultWrapper> parallelActiveCheckExecution(HealthCheckRequest request) {
        return createParallelObservable(request, Schedulers.from(scheduledExecutor))
                .retryWhen(errors -> errors.flatMap(error -> {
                    if (error instanceof RejectedExecutionException) {
                        log.warn("Unable to execute request due to " + error.getMessage());
                        log.warn("delay retry by 1 second");
                        return Observable.timer(1, SECONDS);
                    } else {
                        log.error("Unexpected error during execution", error);
                        return Observable.<Long>empty();
                    }
                }))
                .onErrorReturn(e -> processActiveCheckError(request.getId(), e));
    }

    private Observable<HealthCheckResultWrapper> createParallelObservable(HealthCheckRequest request, Scheduler scheduler) {
        return Observable.just(request)
                .map(this::executeActiveCheck)
                .timeout(executionTimeout.getSeconds(), SECONDS)
                .onErrorReturn(e -> processActiveCheckError(request.getId(), e))
                .subscribeOn(scheduler);
    }

    private HealthCheckResultWrapper processActiveCheckError(HealthCheckID id, Throwable e) {
        HealthImpactMapping impactMapping = checkFunctionsMap.get(id).getImpactMapping();
        if (e instanceof TimeoutException) {
            log.warn("Timeout occurred during invocation {} healthcheck", id);
            return new HealthCheckResultWrapper(new HealthCheckResult(id, HealthStateEnum.Critical, "Execution timed out and terminated"), PERIODIC, executionTimeout, impactMapping);
        } else {
            log.error("Unexpected error during invocation {} healthcheck", id, e);
            return new HealthCheckResultWrapper(new HealthCheckResult(id, HealthStateEnum.Critical, e.getMessage()), PERIODIC, impactMapping);
        }
    }

    // works in multi threads
    private HealthCheckResultWrapper executeActiveCheck(HealthCheckRequest request) {
        return request.execute(clock, log);
    }

    // works in single thread (not thread safe)
    private void processHealthCheckSignal(HealthCheckSignal signal) {
        if (signal == null) {
            log.error("Received null Health Check signal");
            return;
        }
        HealthCheckConfig healthCheckConfig = getConfig();
        switch (signal.getType()) {
            case PERIODIC:
                processResult((HealthCheckResultWrapper) signal, healthCheckConfig);
                reschedule((HealthCheckResultWrapper) signal, healthCheckConfig);
                break;
            case FORCED:
                processResult((HealthCheckResultWrapper) signal, healthCheckConfig);
                break;
            case PASSIVE:
                processResult((HealthCheckResultWrapper) signal, healthCheckConfig);
                break;
            case EXPIRATION_CONTROL:
                processExpirationSignal(healthCheckConfig);
                break;
            case CONFIG_UPDATE:
                HealthCheckConfig newHealthCheckConfig = ((UpdateConfigSignal) signal).getConfig();
                processUpdateConfigSignal(newHealthCheckConfig);
                break;
            default:
                log.warn("Received unsupported event type");
        }
    }

    // works in single thread (not thread safe)
    private void reschedule(HealthCheckResultWrapper result, HealthCheckConfig healthCheckConfig) {
        HealthCheckID id = result.getId();
        Duration delay = (result.getState() == HealthStateEnum.Critical) ? healthCheckConfig.getRetryPeriod(id) : healthCheckConfig.getPeriod(id);
        log.debug("Schedule new execution of {} health check after {}", id, TimeFormatter.printMmSs(delay));
        HealthCheckRequest request = createRequest(checkFunctionsMap.get(id), PERIODIC, healthCheckConfig);
        Observable.timer(delay.toMillis(), MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(i -> rescheduleSubject.onNext(request));
    }

    // works in single thread (not thread safe)
    private void processResult(HealthCheckResultWrapper result, HealthCheckConfig healthCheckConfig) {
        HealthCheckID id = result.getId();
        log.debug("Received {} health check result {} for {} on thread {}", result.getType(), result.getState(), id, Thread.currentThread().getName());
        state.updateStateV1(result, getExpirationPeriod(id, healthCheckConfig));
    }

    // works in single thread (not thread safe)
    private void processExpirationSignal(HealthCheckConfig healthCheckConfig) {
        log.debug("Received expiration check signal");
        checkFunctionsMap.keySet().forEach(id -> state.checkExpired(id, clock.instant(), getExpirationPeriod(id, healthCheckConfig)));
    }

    // works in single thread (not thread safe)
    private void processUpdateConfigSignal(HealthCheckConfig config) {
        log.debug("Setting new Health config");
        this.currentHealthCheckConfig = config;
    }


    @Override
    public void forceCheckSync() throws ForceHealthCheckFailedException {
        HealthCheckConfig config = getConfig();
        log.debug("force sync health check started");
        List<HealthCheckRequest> healthCheckRequests = createHealthCheckRequests(config);
        Observable.from(healthCheckRequests).flatMap(this::parallelForceCheckExecution)
                .toBlocking().subscribe(forceSubject::onNext, e -> {
            throw new ForceHealthCheckFailedException("", e);
        });
        log.debug("force sync health check finished");
    }

    @Override
    public void forceCheckAsync() {
        HealthCheckConfig config = getConfig();
        log.debug("force async health check started");
        List<HealthCheckRequest> healthCheckRequests = createHealthCheckRequests(config);
        Observable.from(healthCheckRequests).flatMap(this::parallelForceCheckExecution)
                .onErrorResumeNext(Observable.never())
                .subscribe(forceSubject::onNext);
        log.debug("force async health check finished");
    }

    private List<HealthCheckRequest> createHealthCheckRequests(HealthCheckConfig config) {
        return checkFunctionsMap.values().stream().map(f -> createRequest(f, FORCED, config)).collect(Collectors.toList());
    }

    private HealthCheckConfig getConfig() {
        return currentHealthCheckConfig;
    }

    @Override
    public void updateConfig(HealthCheckConfig newHealthCheckConfig) {
        log.debug("Notified about Health Config update");
        managementSubject.onNext(new UpdateConfigSignal(newHealthCheckConfig));
    }

    @Override
    public Map<HealthCheckID, LatestHealthCheckState> getAllResults() {
        return Collections.unmodifiableMap(state.getAllResults());
    }

    @Override
    public HealthStateEnum getGlobalState() {
        return state.getGlobalState();
    }

    @Override
    public Instant getLastChanged() {
        return state.getLastChanged();
    }


    private Observable<HealthCheckResultWrapper> parallelForceCheckExecution(HealthCheckRequest request) {
        return createParallelObservable(request, Schedulers.from(forcedExecutor));
    }

    @Override
    public void sendPassiveCheckResult(HealthCheckResult result) {
        passiveSubject.onNext(result);
    }

    private Duration getExpirationPeriod(HealthCheckID id, HealthCheckConfig config) {
        // intentionally takes maximum period to avoid expiration of passive results
        return config.getPeriod(id).plus(expirationPeriodDelta);
    }

}
