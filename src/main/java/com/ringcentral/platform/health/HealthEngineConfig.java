package com.ringcentral.platform.health;

import java.util.stream.LongStream;

public interface HealthEngineConfig {

    java.time.Duration getTickSignalPeriod();

    Integer getScheduledThreadPoolSize();

    Integer getScheduledQueueSize();

    Integer getForcedThreadPoolSize();

    Integer getForcedQueueSize();

    java.time.Duration getExecutionTimeout();

    java.time.Duration getExpirationSignalPeriod();

    java.time.Duration getExpirationPeriodDelta();

    InitialDelayConfig getInitialDelay();

    ScheduledThreadPoolConfig getScheduledThreadPoolConfig();

    String getLoggerName();

    interface InitialDelayConfig {
        LongStream initialDelaysInSeconds(int size);
    }

    interface ScheduledThreadPoolConfig {
    }
}
