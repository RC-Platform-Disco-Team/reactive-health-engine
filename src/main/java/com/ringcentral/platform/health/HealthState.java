package com.ringcentral.platform.health;

import java.time.Duration;

public interface HealthState {

    void updateStateV1(HealthCheckResultWrapper result, Duration expirationPeriod);

    void updateState(HealthCheckResultWrapper result);

    HealthStateEnum getGlobalState();

    java.time.Instant getLastChanged();
}
