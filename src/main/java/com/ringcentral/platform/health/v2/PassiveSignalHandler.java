package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.*;
import com.ringcentral.platform.rx.MapSubject;
import com.ringcentral.platform.rx.SafeMapOperator;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.Subject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ringcentral.platform.health.HealthCheckSignal.Type.PASSIVE;

/**
 * Passive signals handler
 * - filters events not connected any health check
 * - wraps {@link HealthCheckResult} into {@link HealthCheckResultWrapper}
 * - can distinct non-changing events
 */
@Slf4j
public class PassiveSignalHandler {

    /**
     * Create {@link Subject} for passive checks
     *
     * @param functions                map of registered health checks
     * @param filterNonChangingSignals if true, will filter non-changing events
     * @return rx.subjects.Subject
     */
    public static Subject<HealthCheckResult, HealthCheckResultWrapper> createPassiveSubject(List<HealthCheckFunction> functions,
                                                                                            boolean filterNonChangingSignals) {
        final Map<HealthCheckID, HealthCheckFunction> checkMap = convertToMap(functions);
        Func1<Observable<HealthCheckResult>, Observable<HealthCheckResultWrapper>> mapFunction = observable -> {
            Observable<HealthCheckResultWrapper> passiveObservable = observable
                    .filter(filterNonExistingResults(checkMap))
                    .lift(wrapResult(checkMap));
            if (filterNonChangingSignals) {
                return filterNonChangingSignals(passiveObservable);
            }
            return passiveObservable;
        };

        return MapSubject.create(mapFunction);
    }

    private static Map<HealthCheckID, HealthCheckFunction> convertToMap(List<HealthCheckFunction> functions) {
        return Collections.unmodifiableMap(functions == null ? Collections.emptyMap() :
                functions.stream().collect(
                        Collectors.toMap(HealthCheckFunction::getId,
                                t -> t)
                )
        );
    }

    private static SafeMapOperator<HealthCheckResultWrapper, HealthCheckResult> wrapResult(Map<HealthCheckID, HealthCheckFunction> checks) {
        return new SafeMapOperator<>(false,
                result -> {
                    log.debug("received passive event {} for {}", result.getState(), result.getId());
                    HealthImpactMapping impactMapping = checks.get(result.getId()).getImpactMapping();
                    return new HealthCheckResultWrapper(result, PASSIVE, impactMapping);
                },
                e -> log.warn(e.getMessage(), e));
    }

    private static Func1<HealthCheckResult, Boolean> filterNonExistingResults(Map<HealthCheckID, HealthCheckFunction> checks) {
        return result -> {
            if (!checks.containsKey(result.getId())) {
                log.warn("received passive event {} for non-existing check {}", result.getState(), result.getId());
                return false;
            }
            return true;
        };
    }

    private static Observable<HealthCheckResultWrapper> filterNonChangingSignals(Observable<HealthCheckResultWrapper> passiveObservable) {
        return passiveObservable
                .groupBy(HealthCheckResultWrapper::getId)
                .flatMap(go -> go.distinctUntilChanged(HealthCheckResultWrapper::getState));
    }
}
