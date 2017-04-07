package com.ringcentral.platform.health;

import lombok.Getter;
import lombok.experimental.Delegate;

import java.time.Duration;
import java.util.Optional;

/**
 * Technical wrapper for Health Check result
 */
@Getter
public class HealthCheckResultWrapper extends HealthCheckSignal {

    @Delegate
    private final HealthCheckResult result;
    private final Duration duration;
    private final HealthImpactMapping impactMapping;

    public static HealthCheckResultWrapper initial(HealthCheckFunction function) {
        return new HealthCheckResultWrapper(
                new HealthCheckResult(function.getId(), HealthStateEnum.NA, "Check not performed yet", null),
                HealthCheckSignal.Type.PERIODIC,
                function.getImpactMapping());
    }

    public HealthCheckResultWrapper(HealthCheckResult result, Type type, Duration duration, HealthImpactMapping impactMapping) {
        super(type);
        this.result = result;
        this.duration = duration;
        this.impactMapping = impactMapping;
    }

    public HealthCheckResultWrapper(HealthCheckResult result, Type type, HealthImpactMapping impactMapping) {
        this(result, type, null, impactMapping);
    }

    public Optional<Duration> getDuration() {
        return Optional.ofNullable(duration);
    }

    HealthStateEnum getImpact() {
        return impactMapping.getOrDefault(getState(), getState());
    }
}
