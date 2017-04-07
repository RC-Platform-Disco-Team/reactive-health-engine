package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.HealthCheckConfig;
import com.ringcentral.platform.health.HealthCheckFunction;
import com.ringcentral.platform.health.HealthCheckRequest;
import com.ringcentral.platform.health.HealthCheckSignal.TickSignal;
import com.ringcentral.platform.health.HealthState;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static com.ringcentral.platform.health.HealthCheckSignal.Type.PERIODIC;

/**
 * Class for converting tick signals {@link TickSignal} into exact health check requests {@link HealthCheckRequest}
 * according to current state
 */
//TODO rename
@Slf4j
class HealthCheckSplitter {

    private List<HealthCheckFunction> checks;
    private HealthState state;

    HealthCheckSplitter(List<HealthCheckFunction> checks, HealthState state) {
        this.checks = checks;
        this.state = state;
    }

    /**
     * This method is executed for every {@link TickSignal} and go through full list of Health Checks
     * and determine which of them has to be executed
     *
     * @param signal tick signal
     * @return observable emitting health check requests
     */
    Observable<HealthCheckRequest> convertTickToRequest(TickSignal signal) {
        if (signal.getHealthCheckConfig() == null) {
            log.warn("Received tick signal with absent config");
            return Observable.empty();
        }
        return Observable.from(checks)
                .filter(Objects::nonNull)
                .filter(f -> isActiveCheckNeeded(f, state))
                .map(f -> createRequest(signal.getHealthCheckConfig(), f));
    }

    private boolean isActiveCheckNeeded(HealthCheckFunction f, HealthState state) {
        //TODO implement state.needCall(f) or something like this
        return true;
    }

    private HealthCheckRequest createRequest(HealthCheckConfig config, HealthCheckFunction f) {
        Boolean disabled = config.isDisabled(f.getId());
        Duration slowTimeout = config.getSlowTimeout(f.getId());
        return new HealthCheckRequest(f, PERIODIC, disabled, slowTimeout);
    }

}
