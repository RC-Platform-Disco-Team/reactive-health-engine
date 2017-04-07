package com.ringcentral.platform.health;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

import java.time.Duration;
import java.util.Random;
import java.util.stream.LongStream;

@Getter
@Builder(builderClassName = "HealthEngineConfigBuilder")
public class HealthEngineConfigImpl implements HealthEngineConfig {

    private final String loggerName;
    private final Duration tickSignalPeriod;
    @Deprecated
    private final Integer scheduledThreadPoolSize;
    @Deprecated
    private final Integer scheduledQueueSize;
    private final Integer forcedThreadPoolSize;
    private final Integer forcedQueueSize;
    private final Duration executionTimeout;
    private final Duration expirationSignalPeriod;
    private final Duration expirationPeriodDelta;
    private final InitialDelayConfig initialDelay;
    private final ScheduledThreadPoolConfig scheduledThreadPoolConfig;

    @SuppressWarnings("unused")
    public static class HealthEngineConfigBuilder {
        private String loggerName = "health";
        @Deprecated
        private Integer scheduledThreadPoolSize = 10;
        @Deprecated
        private Integer scheduledQueueSize = 100;
        private Integer forcedThreadPoolSize = 10;
        private Integer forcedQueueSize = 100;
        private Duration tickSignalPeriod = Duration.ofSeconds(10);
        private Duration executionTimeout = Duration.ofSeconds(50);
        private Duration expirationSignalPeriod = Duration.ofSeconds(90);
        private Duration expirationPeriodDelta = Duration.ofSeconds(20);
        private InitialDelayConfig initialDelay = new FixedInitialDelay(Duration.ZERO);
        private ScheduledThreadPoolConfig scheduledThreadPoolConfig = new FixedSizeScheduledPoolConfig(10, 100);

        public HealthEngineConfigBuilder fixedInitialDelay(Duration initialDelay) {
            this.initialDelay = new FixedInitialDelay(initialDelay);
            return this;
        }

        public HealthEngineConfigBuilder randomInitialDelay(Duration lowerBoundInclusive, Duration upperBoundExclusive) {
            this.initialDelay = new RandomInitialDelay(lowerBoundInclusive, upperBoundExclusive);
            return this;
        }
    }

    LongStream initialDelaysInSeconds(int length) {
        return initialDelay.initialDelaysInSeconds(length);
    }

    @AllArgsConstructor
    private static class FixedInitialDelay implements InitialDelayConfig {

        private final Duration delay;

        @Override
        public LongStream initialDelaysInSeconds(int size) {
            return LongStream.generate(() -> (int) delay.getSeconds()).limit(size);
        }
    }

    @AllArgsConstructor
    private static class RandomInitialDelay implements InitialDelayConfig {

        private final Duration lowerBoundInclusive;
        private final Duration upperBoundExclusive;

        @Override
        public LongStream initialDelaysInSeconds(int size) {
            return new Random().longs(size, lowerBoundInclusive.getSeconds(), upperBoundExclusive.getSeconds());
        }

    }

    @AllArgsConstructor
    @Getter
    static class HealthCheckPerThreadScheduledPoolConfig implements ScheduledThreadPoolConfig {
        private final Integer queueSize;
    }

    @AllArgsConstructor
    @Getter
    static class FixedSizeScheduledPoolConfig implements ScheduledThreadPoolConfig {

        private final Integer poolSize;
        private final Integer queueSize;
    }

}
