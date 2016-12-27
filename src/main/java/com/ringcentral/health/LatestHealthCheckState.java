package com.ringcentral.health;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Getter
public class LatestHealthCheckState {

    private final HealthCheckResultWrapper lastCheckResult;
    private final Instant lastChanged;
    private final Instant expirationTime;

    LatestHealthCheckState(HealthCheckResultWrapper lastCheckResult, Instant lastChanged, Instant expirationTime) {
        this.lastCheckResult = lastCheckResult;
        this.lastChanged = lastChanged;
        this.expirationTime = expirationTime;
    }

    public HealthStateEnum getState() {
        return lastCheckResult.getState();
    }

    public String getMessage() {
        return lastCheckResult.getMessage();
    }

    public HealthCheckID getId() {
        return lastCheckResult.getId();
    }

    public Optional<Duration> getDuration() {
        return lastCheckResult.getDuration();
    }

    public Instant getLastExecuted() {
        return lastCheckResult.getExecutedAt();
    }

    HealthStateEnum getImpact() {
        return lastCheckResult.getImpact();
    }
}
