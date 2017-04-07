package com.ringcentral.platform.health;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Getter
@AllArgsConstructor
public class LatestHealthCheckState {

    private final HealthCheckResultWrapper lastCheckResult;
    private final Instant lastChanged;
    private final Instant expirationTime;

    public LatestHealthCheckState(HealthCheckResultWrapper lastCheckResult, Instant lastChanged) {
        this.lastCheckResult = lastCheckResult;
        this.lastChanged = lastChanged;
        this.expirationTime = null;
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
