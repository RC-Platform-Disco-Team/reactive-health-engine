package com.ringcentral.health

import spock.lang.Specification

import java.time.Duration
import java.time.Instant

class HealthStateTest extends Specification {

    def "should set global health to the highest severity of results"() {

        given:
        def id1 = new HealthCheckID('1')
        def id2 = new HealthCheckID('2')

        def checks = ['1': id1, '2': id2]

        def state = createStateFromChecks(checks)

        when:
        sendCheckResult(state, id1, HealthStateEnum.Critical)
        sendCheckResult(state, id2, HealthStateEnum.Warning)

        then:
        state.globalState == HealthStateEnum.Critical

        when:
        sendCheckResult(state, id1, HealthStateEnum.OK)
        sendCheckResult(state, id2, HealthStateEnum.Warning)

        then:
        state.globalState == HealthStateEnum.Warning

        when:
        sendCheckResult(state, id1, HealthStateEnum.OK)
        sendCheckResult(state, id2, HealthStateEnum.OK)

        then:
        state.globalState == HealthStateEnum.OK
    }

    def "impact, if specified, should take precedence over real check result"() {

        given:
        def id1 = new HealthCheckID('1')
        def id2 = new HealthCheckID('2')

        def checks = ['1': id1, '2': id2]

        def state = createStateFromChecks(checks)

        when:
        sendCheckResult(state, id1, HealthStateEnum.Critical, HealthImpactMapping.SUPPRESS_CRITICAL_MAPPING)
        sendCheckResult(state, id2, HealthStateEnum.Warning)

        then:
        state.globalState == HealthStateEnum.Warning

        when:
        sendCheckResult(state, id1, HealthStateEnum.OK)
        sendCheckResult(state, id2, HealthStateEnum.Warning, HealthImpactMapping.SUPPRESS_CRITICAL_MAPPING)

        then:
        state.globalState == HealthStateEnum.Warning
    }

    def "should set expired checks to Critical regardless of impact mapping"() {

        given:
        def id1 = new HealthCheckID('1')
        def id2 = new HealthCheckID('2')

        def checks = ['1': id1, '2': id2]

        def state = createStateFromChecks(checks)

        when:
        sendCheckResult(state, id1, HealthStateEnum.OK, HealthImpactMapping.DEFAULT_IMPACT_MAPPING, Duration.ofHours(2))
        sendCheckResult(state, id2, HealthStateEnum.OK, HealthImpactMapping.SUPPRESS_CRITICAL_MAPPING, Duration.ofHours(-1))
        state.checkExpired(id1, Instant.now(), Duration.ofMinutes(59))
        state.checkExpired(id2, Instant.now(), Duration.ofMinutes(59))

        then:
        state.globalState == HealthStateEnum.Critical
        state.allResults[id1].state == HealthStateEnum.OK
        state.allResults[id2].state == HealthStateEnum.Critical
        state.allResults[id2].message.contains("expired")
    }

    def createStateFromChecks(Map<String, HealthCheckID> checks) {
        def wrappedChecks = [:]
        checks.each { k, v ->
            wrappedChecks[v] = new LatestHealthCheckState(
                    new HealthCheckResultWrapper(new HealthCheckResult(v, HealthStateEnum.OK, "")
                            , HealthCheckSignal.Type.PERIODIC, Duration.ZERO, HealthImpactMapping.DEFAULT_IMPACT_MAPPING
                    ), Instant.now(), Instant.now()
            )
        }
        return new HealthState(wrappedChecks)
    }

    def sendCheckResult(HealthState health, HealthCheckID id, HealthStateEnum state,
                        HealthImpactMapping impactMapping = HealthImpactMapping.DEFAULT_IMPACT_MAPPING,
                        Duration expirationPeriod = Duration.ZERO) {
        def result = new HealthCheckResult(id, state, 'unused')
        def wrapper = new HealthCheckResultWrapper(result, HealthCheckSignal.Type.PERIODIC, impactMapping)
        health.updateState(wrapper, expirationPeriod)
    }

}
