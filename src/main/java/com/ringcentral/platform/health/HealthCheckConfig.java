package com.ringcentral.platform.health;

import java.time.Duration;

public interface HealthCheckConfig {

    // values for single checks
    Duration getPeriod(HealthCheckID checkId);
    Duration getRetryPeriod(HealthCheckID checkId);
    Duration getSlowTimeout(HealthCheckID checkId);
    boolean isDisabled(HealthCheckID checkId);

}
