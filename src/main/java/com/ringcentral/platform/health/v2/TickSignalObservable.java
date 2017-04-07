package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.HealthCheckConfig;
import com.ringcentral.platform.health.HealthCheckSignal;
import com.ringcentral.platform.health.HealthCheckSignal.TickSignal;
import com.ringcentral.platform.health.HealthEngineConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Scheduler;

import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This class creates initial observable that emits {@link TickSignal} signals periodically
 * according to Health Engine configuration
 */
@Slf4j
public class TickSignalObservable {

    /**
     * Creates {@link Observable<TickSignal>}
     *
     * @param engineCfg            health engine configuration
     * @param healthConfigProvider provider for health checks configuration
     * @param scheduler            scheduler for emitting thread
     * @return observable
     */
    static Observable<TickSignal> createObservable(HealthEngineConfig engineCfg,
                                                   Supplier<HealthCheckConfig> healthConfigProvider,
                                                   Scheduler scheduler) {
        if (engineCfg == null) {
            throw new IllegalStateException("Health engine config cannot be null");
        }
        if (healthConfigProvider == null) {
            throw new IllegalStateException("Health engine config cannot be null");
        }
        return Observable.interval(engineCfg.getTickSignalPeriod().getSeconds(), SECONDS, scheduler).map(l -> sendTick(healthConfigProvider));
    }

    private static HealthCheckSignal.TickSignal sendTick(Supplier<HealthCheckConfig> healthConfigProvider) {
        log.debug("Sending tick signal");
        return HealthCheckSignal.tick(healthConfigProvider.get());
    }

}
