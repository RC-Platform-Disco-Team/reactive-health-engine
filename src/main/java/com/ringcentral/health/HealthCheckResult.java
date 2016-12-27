package com.ringcentral.health;

import lombok.Getter;

import java.time.Instant;

@Getter
public class HealthCheckResult {

    private final HealthCheckID id;
    private final HealthStateEnum state;
    private final String message;
    private final Instant executedAt;

    public HealthCheckResult(HealthCheckID id, HealthStateEnum state, String message) {
        this(id, state, message, Instant.now());
    }

    protected HealthCheckResult(HealthCheckID id, HealthStateEnum state, String message, Instant executedAt) {
        this.id = id;
        this.state = state;
        this.message = message;
        this.executedAt = executedAt;
    }

    @Override
    public String toString() {
        if (message == null || message.isEmpty()) {
            return id + ": " + state.name();
        } else {
            return id + ": " + state.name() + " - " + message;
        }
    }
}
