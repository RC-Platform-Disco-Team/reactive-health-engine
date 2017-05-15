package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.HealthCheckID;
import com.ringcentral.platform.health.HealthCheckResultWrapper;
import com.ringcentral.platform.health.HealthCheckSignal;
import com.ringcentral.platform.health.HealthState;
import com.ringcentral.platform.rx.RealSafeSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Subscriber;

@Slf4j
@RequiredArgsConstructor
public class HealthResultsAnalyzer extends Subscriber<HealthCheckSignal> {

    private final HealthState state;

    @Override
    public void onCompleted() {
        //do nothing
    }

    @Override
    public void onError(Throwable e) {
        log.warn(e.getMessage(), e);
    }

    @Override
    public void onNext(HealthCheckSignal signal) {
        if (signal == null) {
            log.error("Received null Health Check signal");
            return;
        }
        switch (signal.getType()) {
            case PERIODIC:
            case FORCED:
            case PASSIVE:
                processResult((HealthCheckResultWrapper) signal);
                break;
            default:
                log.warn("Received unsupported event type");
        }
    }

    private void processResult(HealthCheckResultWrapper result) {
        HealthCheckID id = result.getId();
        log.debug("Received {} health check result {} for {} on thread {}", result.getType(), result.getState(), id, Thread.currentThread().getName());
        state.updateState(result);
    }

    public static Subscriber<HealthCheckSignal> create(HealthState state) {
        return new RealSafeSubscriber<>(new HealthResultsAnalyzer(state));
    }
}