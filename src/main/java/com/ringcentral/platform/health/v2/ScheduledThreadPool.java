package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.HealthCheckID;
import rx.Scheduler;

import java.util.Optional;

interface ScheduledThreadPool {

    Optional<Scheduler> getScheduler(HealthCheckID id);
}
