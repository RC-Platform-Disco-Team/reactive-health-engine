package com.ringcentral.platform.health;

import com.ringcentral.platform.health.HealthCheckSignal.Type;
import lombok.Value;
import org.slf4j.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Wrapper for 1 Health Check execution
 */
@Value
public class HealthCheckRequest {

    private final HealthCheckFunction function;
    private final Type type;
    private final boolean disabledByConfig;
    private final Duration slowTimeout;

    public HealthCheckID getId() {
        return function.getId();
    }

    public HealthCheckResultWrapper execute(Clock clock, Logger log) {

        if (!function.isEnabled()) {
            log.debug("{} healthcheck is disabled programmatically", getId());
            return new HealthCheckResultWrapper(function.disabled(), type, function.getImpactMapping());
        }
        if (disabledByConfig) {
            log.debug("{} healthcheck is disabled in configuration file", getId());
            return new HealthCheckResultWrapper(function.disabled(), type, function.getImpactMapping());
        }

        HealthCheckResult result;
        Instant start = clock.instant();
        try {
            log.debug("Executing {} check as {}", getId(), type);
            result = function.checkHealth();
        } catch (Exception e) {
            log.error("Exception during {} healthcheck:", getId(), e);
            result = function.critical(e);
        }
        Instant end = clock.instant();
        Duration executionTime = Duration.between(start, end);

        if (result.getState() == HealthStateEnum.OK && executionTime.compareTo(slowTimeout) > 0) {
            String prettyExecutionTime = TimeFormatter.printSsMs(executionTime);
            log.warn("Slow execution of {} healthcheck: {}", function.getId(), prettyExecutionTime);
            result = function.warning("Slow execution: " + prettyExecutionTime);
        }

        log.debug("{} healthcheck is {}", getId(), result.getState());
        return new HealthCheckResultWrapper(result, type, executionTime, function.getImpactMapping());
    }
}
