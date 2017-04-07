package com.ringcentral.platform.health;

public interface HealthCheckFunction {

    HealthCheckID getId();

    HealthCheckResult checkHealth() throws Exception;

    default boolean isEnabled() {
        return true;
    }

    default HealthCheckResult ok() {
        return new HealthCheckResult(getId(), HealthStateEnum.OK, "");
    }

    default HealthCheckResult warning(String message) {
        return new HealthCheckResult(getId(), HealthStateEnum.Warning, message);
    }

    default HealthCheckResult critical(String message) {
        return new HealthCheckResult(getId(), HealthStateEnum.Critical, message);
    }

    default HealthCheckResult critical(Throwable e) {
        return new HealthCheckResult(getId(), HealthStateEnum.Critical, String.format("%s: %s", e.getClass().getName(), e.getMessage()));
    }

    default HealthCheckResult disabled() {
        return new HealthCheckResult(getId(), HealthStateEnum.Disabled, "");
    }

    default HealthImpactMapping getImpactMapping() {
        return HealthImpactMapping.DEFAULT_IMPACT_MAPPING;
    }



}
