package com.ringcentral.health;

import lombok.Getter;

public abstract class HealthCheckSignal {

    @Getter
    private final Type type;

    HealthCheckSignal(Type type) {
        this.type = type;
    }

    enum Type {
        PERIODIC,
        FORCED,
        PASSIVE,
        EXPIRATION_CONTROL,
        CONFIG_UPDATE
    }

    private static final HealthCheckSignal EXPIRATION_CONTROL_SIGNAL = new HealthCheckSignal(Type.EXPIRATION_CONTROL) {};

    public static HealthCheckSignal expiration() {
        return EXPIRATION_CONTROL_SIGNAL;
    }
}


