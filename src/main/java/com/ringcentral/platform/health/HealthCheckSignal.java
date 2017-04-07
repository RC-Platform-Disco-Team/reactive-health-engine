package com.ringcentral.platform.health;

import lombok.Getter;

public abstract class HealthCheckSignal {

    @Getter
    private final Type type;

    HealthCheckSignal(Type type) {
        this.type = type;
    }

    public enum Type {
        PERIODIC,
        FORCED,
        PASSIVE,
        EXPIRATION_CONTROL,
        CONFIG_UPDATE,
        TICK
    }

    private static final HealthCheckSignal EXPIRATION_CONTROL_SIGNAL = new HealthCheckSignal(Type.EXPIRATION_CONTROL) {};

    public static HealthCheckSignal expiration() {
        return EXPIRATION_CONTROL_SIGNAL;
    }

    public static TickSignal tick(HealthCheckConfig healthCheckConfig) {
        return new TickSignal(healthCheckConfig);
    }

    public static class TickSignal extends HealthCheckSignal {

        @Getter private final HealthCheckConfig healthCheckConfig;

        private TickSignal(HealthCheckConfig healthCheckConfig) {
            super(Type.TICK);
            this.healthCheckConfig = healthCheckConfig;
        }
    }
}


