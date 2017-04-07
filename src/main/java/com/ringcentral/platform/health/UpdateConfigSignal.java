package com.ringcentral.platform.health;

import lombok.Getter;

import static com.ringcentral.platform.health.HealthCheckSignal.Type.CONFIG_UPDATE;

class UpdateConfigSignal extends HealthCheckSignal {

    @Getter
    private final HealthCheckConfig config;

    UpdateConfigSignal(HealthCheckConfig updatedConfig) {
        super(CONFIG_UPDATE);
        this.config = updatedConfig;
    }
}
