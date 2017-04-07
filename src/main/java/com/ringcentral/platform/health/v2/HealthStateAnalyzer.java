package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Subscriber;

import java.util.function.Supplier;

@Slf4j
@AllArgsConstructor
public class HealthStateAnalyzer extends Subscriber<HealthCheckSignal> {

    private Supplier<HealthCheckConfig> healthCheckConfig;
    private HealthState state;

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
}
