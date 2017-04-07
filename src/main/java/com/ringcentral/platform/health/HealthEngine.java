package com.ringcentral.platform.health;

import rx.Observable;

import java.time.Instant;
import java.util.Map;

public interface HealthEngine {

    void updateConfig(HealthCheckConfig config);

    void sendPassiveCheckResult(HealthCheckResult result);

    Map<HealthCheckID, LatestHealthCheckState> getAllResults();

    HealthStateEnum getGlobalState();

    Instant getLastChanged();

    void forceCheckSync();

    void forceCheckAsync();

    void subscribeOnPassive(Observable<HealthCheckResult> passiveStream);
}
