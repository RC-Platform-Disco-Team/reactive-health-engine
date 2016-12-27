package com.ringcentral.health;

import lombok.Getter;

import javax.annotation.concurrent.NotThreadSafe;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static com.ringcentral.health.HealthCheckSignal.Type.PERIODIC;
import static com.ringcentral.health.HealthStateEnum.*;

@NotThreadSafe
class HealthState {

    private final Map<HealthCheckID, LatestHealthCheckState> stateHolder;
    @Getter
    private HealthStateEnum globalState = OK;
    @Getter
    private Instant lastChanged;

    HealthState(Map<HealthCheckID, LatestHealthCheckState> states) {
        stateHolder = new TreeMap<>(states);
    }

    Map<HealthCheckID, LatestHealthCheckState> getAllResults() {
        return Collections.unmodifiableMap(stateHolder);
    }

    // supposed to be called from 1 thread
    void updateState(HealthCheckResultWrapper result, Duration expirationPeriod) {
        LatestHealthCheckState oldCheckState = stateHolder.get(result.getId());
        LatestHealthCheckState newCheckState = calculateNewCheckState(oldCheckState, result, expirationPeriod);
        stateHolder.replace(result.getId(), newCheckState);
        globalState = calculateNewGlobalState(result);
    }

    void checkExpired(HealthCheckID id, Instant currentTime, Duration expirationPeriod) {
        LatestHealthCheckState oldState = stateHolder.get(id);
        if (oldState.getExpirationTime().isBefore(currentTime)) {
            HealthCheckResult result = new HealthCheckResult(id, Critical, "Check result expired");
            updateState(new HealthCheckResultWrapper(result, PERIODIC, HealthImpactMapping.DEFAULT_IMPACT_MAPPING), expirationPeriod);
        }
    }

    private LatestHealthCheckState calculateNewCheckState(LatestHealthCheckState oldState, HealthCheckResultWrapper newCheckResult, Duration expirationPeriod) {
        Instant newLastExecuted = newCheckResult.getExecutedAt();
        Instant newLastChanged = newCheckResult.getState() != oldState.getState() ? newLastExecuted : oldState.getLastChanged();
        return new LatestHealthCheckState(newCheckResult, newLastChanged, newLastExecuted.plus(expirationPeriod));
    }

    private HealthStateEnum calculateNewGlobalState(HealthCheckResultWrapper newResult) {
        HealthStateEnum newGlobalState = OK;
        if (stateHolder.values().stream().anyMatch(entry -> entry.getImpact() == Critical)) {
            newGlobalState = Critical;
        } else if (stateHolder.values().stream().anyMatch(entry -> entry.getImpact() == Warning)) {
            newGlobalState = Warning;
        }
        if (newGlobalState != globalState) {
            lastChanged = newResult.getExecutedAt();
        }
        return newGlobalState;
    }

}
