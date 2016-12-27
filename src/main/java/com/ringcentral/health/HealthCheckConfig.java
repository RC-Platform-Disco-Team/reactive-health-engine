package com.ringcentral.health;

import java.time.Duration;

public interface HealthCheckConfig {

    // values for single checks
    Duration getPeriod(HealthCheckID checkId);
    Duration getRetryPeriod(HealthCheckID checkId);
    Duration getSlowTimeout(HealthCheckID checkId);
    Boolean isDisabled(HealthCheckID checkId);

}
