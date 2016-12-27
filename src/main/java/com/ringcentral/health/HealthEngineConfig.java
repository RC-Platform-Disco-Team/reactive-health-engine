package com.ringcentral.health;

import lombok.Getter;
import lombok.experimental.Builder;

import java.time.Duration;
import java.util.Random;
import java.util.stream.LongStream;

@Getter
@Builder(builderClassName = "HealthEngineConfigBuilder")
public class HealthEngineConfig {

    private final Integer scheduledThreadPoolSize;
    private final Integer scheduledQueueSize;
    private final Integer forcedThreadPoolSize;
    private final Integer forcedQueueSize;
    private final Duration executionTimeout;
    private final Duration expirationSignalPeriod;
    private final Duration expirationPeriodDelta;
    private final InitialDelay initialDelay;
    private final String loggerName;

    @SuppressWarnings("unused")
    public static class HealthEngineConfigBuilder {
        private Integer scheduledThreadPoolSize = 10;
        private Integer scheduledQueueSize = 100;
        private Integer forcedThreadPoolSize = 10;
        private Integer forcedQueueSize = 100;
        private Duration executionTimeout = Duration.ofSeconds(50);
        private Duration expirationSignalPeriod = Duration.ofSeconds(90);
        private Duration expirationPeriodDelta = Duration.ofSeconds(20);
        private InitialDelay initialDelay = new FixedInitialDelay(Duration.ZERO);
        private String loggerName = "health";

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


    private interface InitialDelay {
        LongStream initialDelaysInSeconds(int size);
    }

    private static class FixedInitialDelay implements InitialDelay {

        private final Duration delay;

        FixedInitialDelay(Duration delay) {
            this.delay = delay;
        }

        @Override
        public LongStream initialDelaysInSeconds(int size) {
            return LongStream.generate(() -> (int) delay.getSeconds()).limit(size);
        }
    }

    private static class RandomInitialDelay implements InitialDelay {

        private final Duration lowerBoundInclusive;
        private final Duration upperBoundExclusive;

        RandomInitialDelay(Duration lowerBoundInclusive, Duration upperBoundExclusive) {
            this.lowerBoundInclusive = lowerBoundInclusive;
            this.upperBoundExclusive = upperBoundExclusive;
        }

        @Override
        public LongStream initialDelaysInSeconds(int size) {
            return new Random().longs(size, lowerBoundInclusive.getSeconds(), upperBoundExclusive.getSeconds());
        }

    }

}
