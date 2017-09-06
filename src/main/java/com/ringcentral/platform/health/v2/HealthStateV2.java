package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.*;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class HealthStateV2 implements HealthState {

    private final Map<HealthCheckID, LatestHealthCheckState> stateHolder;
    @Getter
    private HealthStateEnum globalState = HealthStateEnum.OK;
    @Getter
    private Instant lastChanged;

    public HealthStateV2(List<HealthCheckFunction> checks, Instant now) {
        this(checks == null ? Collections.emptyMap() :
                convertToMap(checks, now), now);
    }

    private static Map<HealthCheckID, LatestHealthCheckState> convertToMap(List<HealthCheckFunction> checks, Instant now) {
        return checks.stream().collect(Collectors.toMap(
                HealthCheckFunction::getId,
                f -> new LatestHealthCheckState(HealthCheckResultWrapper.initial(f), now)
        ));
    }

    private HealthStateV2(Map<HealthCheckID, LatestHealthCheckState> states, Instant now) {
        this.stateHolder = new TreeMap<>(states);
        this.lastChanged = now;
    }

    @Override
    public void updateStateV1(HealthCheckResultWrapper result, Duration expirationPeriod) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateState(HealthCheckResultWrapper result) {

    }

    public Map<HealthCheckID, LatestHealthCheckState> getDetails() {
        return Collections.unmodifiableMap(stateHolder);
    }
}
