package com.ringcentral.platform.health;

import lombok.Getter;

import javax.annotation.concurrent.NotThreadSafe;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@NotThreadSafe
public class HealthStateV1 implements HealthState {

    private final Map<HealthCheckID, LatestHealthCheckState> stateHolder;
    @Getter
    private HealthStateEnum globalState = HealthStateEnum.OK;
    @Getter
    private Instant lastChanged;

    public HealthStateV1(List<HealthCheckFunction> checks, Instant now) {
        this(checks.stream().collect(
                Collectors.toMap(HealthCheckFunction::getId,
                        f -> new LatestHealthCheckState(HealthCheckResultWrapper.initial(f), now)
                )
        ), now);
    }

    public HealthStateV1() {
        stateHolder = Collections.emptyMap();
    }

    public HealthStateV1(Map<HealthCheckID, LatestHealthCheckState> states, Instant now) {
        stateHolder = new TreeMap<>(states);
        this.lastChanged = now;
    }

    Map<HealthCheckID, LatestHealthCheckState> getAllResults() {
        return Collections.unmodifiableMap(stateHolder);
    }

    // supposed to be called from 1 thread
    @Override
    public void updateStateV1(HealthCheckResultWrapper result, Duration expirationPeriod) {
        LatestHealthCheckState oldCheckState = stateHolder.get(result.getId());
        LatestHealthCheckState newCheckState = calculateNewCheckState(oldCheckState, result, expirationPeriod);
        stateHolder.replace(result.getId(), newCheckState);
        globalState = calculateNewGlobalState(result);
    }

    @Override
    public void updateState(HealthCheckResultWrapper result) {
        if (!stateHolder.containsKey(result.getId())) {
            return;
        }
        LatestHealthCheckState oldCheckState = stateHolder.get(result.getId());
        LatestHealthCheckState newCheckState = calculateNewCheckState(oldCheckState, result);
        stateHolder.replace(result.getId(), newCheckState);
        globalState = calculateNewGlobalState(result);
    }

    private LatestHealthCheckState calculateNewCheckState(LatestHealthCheckState oldState, HealthCheckResultWrapper newCheckResult) {
        Instant newLastExecuted = newCheckResult.getExecutedAt();
        Instant newLastChanged = newCheckResult.getState() != oldState.getState() ? newLastExecuted : oldState.getLastChanged();
        return new LatestHealthCheckState(newCheckResult, newLastChanged);
    }

    void checkExpired(HealthCheckID id, Instant currentTime, Duration expirationPeriod) {
        LatestHealthCheckState oldState = stateHolder.get(id);
        if (oldState.getExpirationTime().isBefore(currentTime)) {
            HealthCheckResult result = new HealthCheckResult(id, HealthStateEnum.Critical, "Check result expired");
            updateStateV1(new HealthCheckResultWrapper(result, HealthCheckSignal.Type.PERIODIC, HealthImpactMapping.DEFAULT_IMPACT_MAPPING), expirationPeriod);
        }
    }

    private LatestHealthCheckState calculateNewCheckState(LatestHealthCheckState oldState, HealthCheckResultWrapper newCheckResult, Duration expirationPeriod) {
        Instant newLastExecuted = newCheckResult.getExecutedAt();
        Instant newLastChanged = newCheckResult.getState() != oldState.getState() ? newLastExecuted : oldState.getLastChanged();
        return new LatestHealthCheckState(newCheckResult, newLastChanged, newLastExecuted.plus(expirationPeriod));
    }

    private HealthStateEnum calculateNewGlobalState(HealthCheckResultWrapper newResult) {
        HealthStateEnum newGlobalState = HealthStateEnum.OK;
        if (stateHolder.values().stream().anyMatch(entry -> entry.getImpact() == HealthStateEnum.Critical)) {
            newGlobalState = HealthStateEnum.Critical;
        } else if (stateHolder.values().stream().anyMatch(entry -> entry.getImpact() == HealthStateEnum.Warning)) {
            newGlobalState = HealthStateEnum.Warning;
        }
        if (newGlobalState != globalState) {
            lastChanged = newResult.getExecutedAt();
        }
        return newGlobalState;
    }

}
